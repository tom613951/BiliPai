package com.android.purebilibili.build;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compose "detached-root owner" guard.
 *
 * <p>Upstream issue #880: during a fullscreen switch while a SharedTransition morph is still
 * animating, a LayoutNode is detached from its Owner yet is measured again in the same frame.
 * {@code androidx.compose.ui.node.LayoutNodeKt.requireOwner} then throws
 *
 * <pre>java.lang.IllegalStateException: LayoutNode should be attached to an owner</pre>
 *
 * <p>The throwing method is package-private library code, so the guard is applied as a
 * post-compile bytecode rewrite of exactly one call site: the null branch inside
 * {@code LayoutNodeKt.requireOwner(LayoutNode)}. Instead of falling through to
 * {@code throwIllegalStateExceptionForNullCheck}, that branch is diverted to
 * {@code DetachedOwnerFallback.ownerOrRethrow}, which returns the node's last known Owner or
 * rethrows the original error when none was ever seen.
 *
 * <p>Scope is deliberately narrow: only {@code androidx/compose/ui/node/LayoutNodeKt.class} is
 * rewritten, and only that single branch. Every other class stays byte-identical.
 */
public class ComposeDetachedOwnerGuardPlugin implements Plugin<Project> {

    private static final String TARGET_CLASS = "androidx/compose/ui/node/LayoutNodeKt.class";
    private static final String TARGET_INTERNAL = "androidx/compose/ui/node/LayoutNodeKt";
    private static final String HELPER_OWNER = "com/android/purebilibili/build/DetachedOwnerFallback";
    private static final String HELPER_DESC =
            "(Landroidx/compose/ui/node/LayoutNode;Landroidx/compose/ui/node/Owner;)Landroidx/compose/ui/node/Owner;";

    @Override
    public void apply(Project project) {
        // The compose-ui AAR reaches the compiler as an extracted classes.jar under
        // ~/.gradle/caches/<gradle-version>/transforms. Patching the AAR in modules-2 is not
        // enough: AGP compiles from the transform output. So the guard rewrites the exact
        // classes.jar that the build consumes, identified by walking the transform cache for
        // the LayoutNodeKt class, and keeps a .cdog-orig backup for repeatability.
        project.getTasks().register("patchComposeDetachedOwnerGuard", PatchTask.class, task -> {
            task.setGroup("build");
            task.setDescription(
                    "Rewrite LayoutNodeKt.requireOwner so a detached-root measure no longer throws (#880).");
            task.getOutputs().upToDateWhen(t -> false);
            task.setGradleUserHome(project.getGradle().getGradleUserHomeDir());
        });

        project.afterEvaluate(p -> p.getTasks()
                .matching(t -> t.getName().startsWith("compile") || t.getName().contains("Kotlin"))
                .configureEach(t -> t.dependsOn("patchComposeDetachedOwnerGuard")));
    }

    /** Serializable task: rewrites the consumed LayoutNodeKt.class in place. */
    public abstract static class PatchTask extends org.gradle.api.DefaultTask {
        private final org.gradle.api.provider.Property<java.io.File> gradleUserHome =
                getProject().getObjects().property(java.io.File.class);

        @org.gradle.api.tasks.Internal
        public org.gradle.api.provider.Property<java.io.File> getGradleUserHome() {
            return gradleUserHome;
        }

        public void setGradleUserHome(java.io.File v) {
            gradleUserHome.set(v);
        }

        @org.gradle.api.tasks.TaskAction
        public void run() throws IOException {
            java.io.File transformsRoot = new java.io.File(
                    new java.io.File(gradleUserHome.get(), "caches"), "9.5.0/transforms");
            if (!transformsRoot.isDirectory()) {
                transformsRoot = new java.io.File(gradleUserHome.get(), "caches");
            }

            java.util.List<java.io.File> targets = new java.util.ArrayList<>();
            java.nio.file.Files.walk(transformsRoot.toPath())
                    .filter(java.nio.file.Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().equals("classes.jar"))
                    .filter(f -> f.toString().replace('\\', '/')
                            .contains("transformed/ui/jars/classes.jar"))
                    .forEach(f -> targets.add(f.toFile()));

            if (targets.isEmpty()) {
                throw new org.gradle.api.GradleException(
                        "[ComposeDetachedOwnerGuard] no compose-ui classes.jar under " + transformsRoot
                                + "; run a build once so the transform cache is populated, then retry.");
            }

            int total = 0;
            for (java.io.File jar : targets) {
                java.io.File backup = new java.io.File(jar.getAbsolutePath() + ".cdog-orig");
                if (!backup.exists()) {
                    java.nio.file.Files.copy(jar.toPath(), backup.toPath());
                }
                java.io.File source = backup;
                try {
                    byte[] original = java.nio.file.Files.readAllBytes(source.toPath());
                    byte[] rewritten = rewriteClassesJar(original, new int[]{0});
                    java.nio.file.Files.write(jar.toPath(), rewritten);
                    total++;
                } catch (IOException e) {
                    throw new org.gradle.api.GradleException(
                            "ComposeDetachedOwnerGuard failed on " + jar, e);
                }
            }
            getLogger().lifecycle("[ComposeDetachedOwnerGuard] patched " + total
                    + " compose-ui classes.jar in the transform cache");
        }
    }

    /** Copies the AAR, rewriting {@code classes.jar} on the way through. */
    private static int patchAar(File aar, File out) throws IOException {
        int[] sites = {0};
        try (ZipFile zip = new ZipFile(aar);
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out))) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                zos.putNextEntry(new ZipEntry(entry.getName()));
                if ("classes.jar".equals(entry.getName())) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        byte[] rewritten = rewriteClassesJar(readAll(in), sites);
                        zos.write(rewritten);
                    }
                } else {
                    try (InputStream in = zip.getInputStream(entry)) {
                        copy(in, zos);
                    }
                }
                zos.closeEntry();
            }
        }
        return sites[0];
    }

    private static byte[] rewriteClassesJar(byte[] jarBytes, int[] sites) throws IOException {
        File tmp = File.createTempFile("cdog", ".jar");
        try {
            try (FileOutputStream fos = new FileOutputStream(tmp)) { fos.write(jarBytes); }
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            try (JarFile jar = new JarFile(tmp);
                 JarOutputStream jos = new JarOutputStream(result)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry e = entries.nextElement();
                    if (e.isDirectory()) continue;
                    byte[] data;
                    try (InputStream in = jar.getInputStream(e)) { data = readAll(in); }
                    if (TARGET_CLASS.equals(e.getName())) {
                        data = rewriteClass(data, sites);
                    }
                    jos.putNextEntry(new JarEntry(e.getName()));
                    jos.write(data);
                    jos.closeEntry();
                }
            }
            return result.toByteArray();
        } finally {
            if (!tmp.delete()) tmp.deleteOnExit();
        }
    }

    /**
     * Redirects the single null-check throw inside requireOwner.
     *
     * <p>Kotlin compiles the null check to:
     * <pre>
     *   invokevirtual LayoutNode.getOwner$ui()
     *   ...
     *   ifnonnull L
     *   ldc  "LayoutNode should be attached to an owner"
     *   invokestatic InlineClassHelperKt.throwIllegalStateExceptionForNullCheck
     * L: ...
     * </pre>
     * The {@code invokestatic} is replaced by a call to the fallback plus an {@code ARETURN},
     * making the throw unreachable on that path.
     */
    private static byte[] rewriteClass(byte[] classBytes, int[] sites) {
        ClassReader reader = new ClassReader(classBytes);
        final ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        final int[] patched = {0};

        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"requireOwner".equals(name)) return mv;

                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String mName,
                                                String mDesc, boolean isInterface) {
                        if ("throwIllegalStateExceptionForNullCheck".equals(mName)) {
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            super.visitVarInsn(Opcodes.ALOAD, 1);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER_OWNER,
                                    "ownerOrRethrow", HELPER_DESC, false);
                            super.visitInsn(Opcodes.ARETURN);
                            patched[0]++;
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, mName, mDesc, isInterface);
                    }
                };
            }
        }, 0);

        sites[0] += patched[0];
        return writer.toByteArray();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        copy(in, bos);
        return bos.toByteArray();
    }

    private static void copy(InputStream in, java.io.OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }
}
