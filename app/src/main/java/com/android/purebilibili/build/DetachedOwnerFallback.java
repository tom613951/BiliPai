package com.android.purebilibili.build;

import android.util.Log;

import androidx.compose.ui.node.LayoutNode;
import androidx.compose.ui.node.Owner;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Runtime side of ComposeDetachedOwnerGuard (see build-logic).
 *
 * <p>The bytecode guard replaces the
 * {@code IllegalStateException("LayoutNode should be attached to an owner")} throw inside
 * {@code LayoutNodeKt.requireOwner} with a call to {@link #ownerOrRethrow}.
 *
 * <p>Upstream issue #880: a fullscreen switch during an in-flight SharedTransition morph can
 * re-measure a node that was already detached from its Owner. That measure pass only needs a
 * non-null Owner in order to read the placement scope / snapshot observer; the node is not
 * placed by it, so the frame completes harmlessly instead of crashing the app.
 *
 * <p>Written in Java on purpose: {@code LayoutNode} and {@code Owner} are declared
 * {@code internal} in Compose UI, so Kotlin refuses to reference them from another module even
 * though they are public in bytecode. Java has no such restriction.
 *
 * <p>A node that truly never had an Owner rethrows the original error, so genuine misuse of
 * Compose is not silently masked.
 */
public final class DetachedOwnerFallback {

    private static final String TAG = "DetachedOwnerGuard";

    /** Last Owner observed per node; weak keys so detached nodes stay collectable. */
    private static final Map<LayoutNode, WeakReference<Owner>> LAST_OWNER = new WeakHashMap<>();

    private DetachedOwnerFallback() {
    }

    /** Kept off in release; enable while diagnosing #880 to see when the guard fires. */
    public static volatile boolean verbose = false;

    /** Records the Owner while the node is still attached, for a later detached measure. */
    public static void remember(LayoutNode node, Owner owner) {
        if (node == null || owner == null) return;
        synchronized (LAST_OWNER) {
            LAST_OWNER.put(node, new WeakReference<>(owner));
        }
    }

    /**
     * Injected entry point. Returns {@code owner} when present, otherwise the node's last known
     * Owner. Throws the original Compose error when neither is available.
     */
    public static Owner ownerOrRethrow(LayoutNode node, Owner owner) {
        if (owner != null) return owner;

        if (node != null) {
            synchronized (LAST_OWNER) {
                WeakReference<Owner> ref = LAST_OWNER.get(node);
                Owner remembered = ref == null ? null : ref.get();
                if (remembered != null) {
                    if (verbose) {
                        Log.w(TAG, "Measured a detached LayoutNode; using its last Owner (upstream issue #880).");
                    }
                    return remembered;
                }
            }
        }

        throw new IllegalStateException("LayoutNode should be attached to an owner");
    }
}
