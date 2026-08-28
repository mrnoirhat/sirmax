// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sirmax.shared;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A minimal success/failure container for application use-case outcomes.
 *
 * <p>Use cases return {@code Result} instead of throwing for expected, user-facing outcomes
 * (validation failures, missing requirements, authorization denials). Unexpected faults still throw.
 *
 * @param <T> the success value type
 */
public sealed interface Result<T> permits Result.Ok, Result.Err {

    static <T> Result<T> ok(T value) {
        return new Ok<>(value);
    }

    static <T> Result<T> err(String code, String messageKey) {
        return new Err<>(code, messageKey);
    }

    boolean isOk();

    default boolean isErr() {
        return !isOk();
    }

    /** The success value, or throws {@link IllegalStateException} if this is an error. */
    T orElseThrow();

    Optional<T> value();

    <R> Result<R> map(Function<? super T, ? extends R> mapper);

    <R> Result<R> flatMap(Function<? super T, Result<R>> mapper);

    T orElseGet(Supplier<? extends T> fallback);

    record Ok<T>(T value) implements Result<T> {
        public Ok {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public boolean isOk() {
            return true;
        }

        @Override
        public T orElseThrow() {
            return value;
        }

        @Override
        public Optional<T> value() {
            return Optional.of(value);
        }

        @Override
        public <R> Result<R> map(Function<? super T, ? extends R> mapper) {
            return new Ok<>(mapper.apply(value));
        }

        @Override
        public <R> Result<R> flatMap(Function<? super T, Result<R>> mapper) {
            return mapper.apply(value);
        }

        @Override
        public T orElseGet(Supplier<? extends T> fallback) {
            return value;
        }
    }

    /**
     * A failed outcome.
     *
     * @param code stable machine-readable code (e.g. {@code "REQUIREMENTS_INCOMPLETE"})
     * @param messageKey i18n key resolved for display; never a literal user string
     */
    record Err<T>(String code, String messageKey) implements Result<T> {
        public Err {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(messageKey, "messageKey");
        }

        @Override
        public boolean isOk() {
            return false;
        }

        @Override
        public T orElseThrow() {
            throw new IllegalStateException("Result is Err(" + code + ")");
        }

        @Override
        public Optional<T> value() {
            return Optional.empty();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> Result<R> map(Function<? super T, ? extends R> mapper) {
            return (Result<R>) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> Result<R> flatMap(Function<? super T, Result<R>> mapper) {
            return (Result<R>) this;
        }

        @Override
        public T orElseGet(Supplier<? extends T> fallback) {
            return fallback.get();
        }
    }
}
