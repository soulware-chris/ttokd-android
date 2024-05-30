package kr.co.miaps.welfare.api;

import androidx.annotation.NonNull;

import java.util.concurrent.CompletableFuture;
import kotlin.Result;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class StringContinuation implements Continuation<String> {
    private final CompletableFuture<String> future;

    public StringContinuation(CompletableFuture<String> future) {
        this.future = future;
    }
    @NonNull
    @Override
    public CoroutineContext getContext() {
        return EmptyCoroutineContext.INSTANCE;
    }

    @Override
    public void resumeWith(@NonNull Object o) {
        if (o instanceof Result.Failure)
            future.completeExceptionally(((Result.Failure) o).exception);
        else
            future.complete((String) o);
    }
}

