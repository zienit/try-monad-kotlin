package nl.zienit.functional

import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

sealed class Try<T> {

    abstract fun isSuccess(): Boolean
    abstract fun isFailure(): Boolean
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
        private inline fun <U> eval(lambda: () -> Try<U>): Try<U> =
            try {
                lambda()
            } catch (e: Throwable) {
                e.failure()
            }

        fun <U> of(lambda: () -> U): Try<U> = eval { lambda().success() }
    }

    class Success<T>(val value: T) : Try<T>() {

        override fun isSuccess(): Boolean = true
        override fun isFailure(): Boolean = false
        override fun get(): T = value
        override fun getException(): Throwable = throw IllegalStateException()
        override fun <U> map(mapper: (T) -> U): Try<U> = eval { Success(mapper(value)) }
        override fun <U> flatMap(mapper: (T) -> Try<out U>): Try<U> = eval { mapper(value) as Try<U> }
        override fun filter(predicate: (T) -> Boolean): Try<T> =
            eval { if (predicate(value)) this else Failure<T>(NoSuchElementException()) }

        override fun recover(mapper: (Throwable) -> T): Try<T> = this
        override fun <E : Throwable> recover(clazz: KClass<E>, mapper: (E) -> T): Try<T> = this
        override fun recoverWith(mapper: (Throwable) -> Try<T>): Try<T> = this
        override fun <E : Throwable> recoverWith(clazz: KClass<E>, mapper: (E) -> Try<T>): Try<T> = this
    }

    class Failure<T>(val ex: Throwable) : Try<T>() {

        override fun isSuccess(): Boolean = false
        override fun isFailure(): Boolean = true
        override fun get(): T = throw ex
        override fun getException(): Throwable = ex
        override fun <U> map(mapper: (T) -> U): Try<U> = this as Try<U>
        override fun <U> flatMap(mapper: (T) -> Try<out U>): Try<U> = this as Try<U>
        override fun filter(predicate: (T) -> Boolean): Try<T> = this
        override fun recover(mapper: (Throwable) -> T): Try<T> = eval { Success(mapper(ex)) }
        override fun <E : Throwable> recover(clazz: KClass<E>, mapper: (E) -> T): Try<T> =
            eval { if (clazz.isSuperclassOf(ex::class)) Success(mapper(ex as E)) else this }

        override fun recoverWith(mapper: (Throwable) -> Try<T>): Try<T> = eval { mapper(ex) }
        override fun <E : Throwable> recoverWith(clazz: KClass<E>, mapper: (E) -> Try<T>): Try<T> =
            eval { if (clazz.isSuperclassOf(ex::class)) mapper(ex as E) else this }
    }
}

fun <T> T.success(): Try<T> = Try.Success(this)
fun <T> Throwable.failure(): Try<T> = Try.Failure<T>(this)

fun <T> Sequence<Try<T>>.toTryList(): Try<List<T>> {
    return toCollection(this.asIterable(), mutableListOf<T>()) as Try<List<T>>
}

fun <T> Collection<Try<T>>.toTryList(): Try<List<T>> {
    return toCollection(this.asIterable(), mutableListOf<T>()) as Try<List<T>>
}

private fun <T> toCollection(from: Iterable<Try<T>>, to: MutableCollection<T>): Try<out Collection<T>> {
    for (item in from) {
        if (item.isFailure()) {
            return item as Try.Failure<List<T>>
        }
        to.add(item.get())
    }
    return to.success()
}
