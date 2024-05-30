package kr.co.miaps.welfare.api;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.ktor.http.Cookie;
import kotlin.Result;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class CookiesContinuation implements Continuation<List<Cookie>> {
    private final CompletableFuture<List<Cookie>> future;

    public CookiesContinuation(CompletableFuture<List<Cookie>> future) {
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
            future.complete(((List<Cookie>) o));
    }
}
