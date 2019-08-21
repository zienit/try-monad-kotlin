package nl.zienit.functional

import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

sealed class Try<T> {

    abstract fun get(): T
    abstract fun getException(): Throwable
    abstract fun <U> map(mapper: (T) -> U): Try<U>
    abstract fun <U> flatMap(mapper: (T) -> Try<out U>): Try<U>
    abstract fun filter(predicate: (T) -> Boolean): Try<T>
    abstract fun recover(mapper: (Throwable) -> T): Try<T>
    abstract fun <E : Throwable> recover(clazz: KClass<E>, mapper: (E) -> T): Try<T>
    abstract fun recoverWith(mapper: (Throwable) -> Try<T>): Try<T>
    abstract fun <E : Throwable> recoverWith(clazz: KClass<E>, mapper: (E) -> Try<T>): Try<T>

    companion object {
        private inline fun <U> eval(lambda: () -> Try<U>): Try<U> {
            try {
                return lambda()
            } catch (e: Throwable) {
                return failure(e)
            }
        }

        fun <U> success(value: U): Try<U> = Success(value)
        fun success(): Try<Nothing?> = Success(null)
        fun <U> failure(exception: Throwable): Try<U> = Failure(exception)
        fun <U> of(lambda: () -> U): Try<U> = eval { success(lambda()) }
    }

    class Success<T>(val value: T) : Try<T>() {

        override fun get(): T = value
        override fun getException(): Throwable = throw IllegalStateException()
        override fun <U> map(mapper: (T) -> U): Try<U> = eval { success(mapper(value)) }
        override fun <U> flatMap(mapper: (T) -> Try<out U>): Try<U> = eval { mapper(value) as Try<U> }
        override fun filter(predicate: (T) -> Boolean): Try<T> =
            eval { if (predicate(value)) this else failure<T>(NoSuchElementException()) }

        override fun recover(mapper: (Throwable) -> T): Try<T> = this
        override fun <E : Throwable> recover(clazz: KClass<E>, mapper: (E) -> T): Try<T> = this
        override fun recoverWith(mapper: (Throwable) -> Try<T>): Try<T> = this
        override fun <E : Throwable> recoverWith(clazz: KClass<E>, mapper: (E) -> Try<T>): Try<T> = this
    }

    class Failure<T>(val ex: Throwable) : Try<T>() {

        override fun get(): T = throw ex
        override fun getException(): Throwable = ex
        override fun <U> map(mapper: (T) -> U): Try<U> = this as Try<U>
        override fun <U> flatMap(mapper: (T) -> Try<out U>): Try<U> = this as Try<U>
        override fun filter(predicate: (T) -> Boolean): Try<T> = this
        override fun recover(mapper: (Throwable) -> T): Try<T> = eval { Try.success(mapper(ex)) }
        override fun <E : Throwable> recover(clazz: KClass<E>, mapper: (E) -> T): Try<T> =
            eval { if (clazz.isSuperclassOf(ex::class)) Try.success(mapper(ex as E)) else this }

        override fun recoverWith(mapper: (Throwable) -> Try<T>): Try<T> = eval { mapper(ex) }
        override fun <E : Throwable> recoverWith(clazz: KClass<E>, mapper: (E) -> Try<T>): Try<T> =
            eval { if (clazz.isSuperclassOf(ex::class)) mapper(ex as E) else this }
    }
}