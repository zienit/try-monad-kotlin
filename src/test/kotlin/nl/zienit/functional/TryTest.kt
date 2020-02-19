package nl.zienit.functional

import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class TryTest {

    open class A(val x: Int) {}
    open class B(x: Int) : A(x) {}
    class C(x: Int) : B(x) {}

    @Test
    fun testSuccess() {
        val t = 1.success()
        assertThat(t.get(), equalTo(1))
    }

    @Test
    fun testSuccessNothing() {
        val t = null.success()
        assertThat(t.get(), equalTo(null))
    }

    @Test
    fun testFailure() {
        val f = IllegalArgumentException().failure<Int>()
        assertThat(f.getException(), instanceOf(IllegalArgumentException::class.java))
    }

    @Test
    fun testOfSuccess() {
        val t = Try.of {
            1
        }
        assertThat(t.get(), equalTo(1))
    }

    @Test
    fun testOfFailure() {
        val f = Try.of {
            throw IllegalArgumentException()
        }
    }

    @Test
    fun testWhen() {
        val t = 1.success()

        val u = when (t) {
            is Try.Success -> t.get()
            is Try.Failure -> 0
        }

        assertThat(u, equalTo(1))
    }

    @Test
    fun testWhenFailure() {
        val t = IllegalArgumentException().failure<Int>()

        val u = when (t) {
            is Try.Success -> t.get()
            is Try.Failure -> 0
        }

        assertThat(u, equalTo(0))
    }

    @Test
    fun testOfMap() {

        val mapper: (A) -> C = { a -> C(a.x) }

        val t: Try<B> = B(1).success()
        val u: Try<A> = t.map(mapper)

        assertThat(u.get().x, equalTo(1))
    }

    @Test
    fun testOfMapFailure() {

        val t = IllegalArgumentException().failure<Int>()
        val u = t.map { it }

        assertThat(u.getException(), instanceOf(IllegalArgumentException::class.java))
    }

    @Test
    fun testOfMapThrows() {

        val t = success()
        val u = t.map { throw IllegalArgumentException() }

        assertThat(u.getException(), instanceOf(IllegalArgumentException::class.java))
    }

    @Test
    fun testOfFlatMap() {

        val mapper: (A) -> Try<C> = { a -> C(a.x).success() }

        val t: Try<B> = B(1).success()
        val u: Try<A> = t.flatMap(mapper)

        assertThat(u.get().x, equalTo(1))
    }

    @Test
    fun testOfFilter() {

        val t: Try<B> = B(1).success()
        val filter: (A) -> Boolean = { true }

        val u: Try<B> = t.filter(filter)

        assertThat(u.get().x, equalTo(1))
    }

    @Test
    fun testOfRecover() {

        val t: Try<B> = NoSuchElementException().failure()
        val u = t.recover { C(2) }

        assertThat(u.get().x, equalTo(2))
    }

    @Test
    fun testOfRecoverClass() {

        val t: Try<B> = NullPointerException("foo").failure()
        val u = t.recover(RuntimeException::class) { e -> C(e.message?.length ?: 0) }

        assertThat(u.get().x, equalTo(3))
    }

    @Test
    fun testOfRecoverWith() {

        val t: Try<B> = NoSuchElementException().failure()
        val u = t.recoverWith { C(2).success() }

        assertThat(u.get().x, equalTo(2))
    }

    @Test
    fun testOfRecoverWithClass() {

        val t: Try<B> = NullPointerException("foo").failure()
        val u = t.recoverWith(RuntimeException::class) { e -> C(e.message?.length ?: 0).success() }

        assertThat(u.get().x, equalTo(3))
    }

    @Test
    fun testGetOrElse() {
        val t: Try<B> = NullPointerException("foo").failure()
        assertThat(t.getOrElse(B(12)).x, equalTo(12))
    }

    @Test
    fun testGetOrNull() {
        val t: Try<B> = NullPointerException("foo").failure()
        assertThat(t.getOrNull(), equalTo<B?>(null))
    }
}