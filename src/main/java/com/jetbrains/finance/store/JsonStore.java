package com.jetbrains.finance.store;

import java.nio.file.Path;

/**
 * Deprecated: kept for backward-compat. Use {@link PlainTextStore}.
 * This wrapper simply delegates to PlainTextStore.
 */
@Deprecated
public class JsonStore extends PlainTextStore {
    public JsonStore(Path file) { super(file); }
}
