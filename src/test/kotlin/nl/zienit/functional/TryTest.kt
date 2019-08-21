package nl.zienit.functional

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class TryTest {

    open class A(val x: Int) {}
    open class B(x: Int) : A(x) {}
    class C(x: Int) : B(x) {}

    @Test
    fun testSuccess() {
        val t = Try.success(1)
        assertThat(t.get(), equalTo(1))
    }

    @Test
    fun testSuccessNothing() {
        val t = Try.success()
        assertThat(t.get(), equalTo(null))
    }

    @Test
    fun testFailure() {
        val f = Try.failure<Int>(IllegalArgumentException())
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
        val t = Try.success(1)

        val u = when (t) {
            is Try.Success -> t.get()
            is Try.Failure -> 0
        }

        assertThat(u, equalTo(1))
    }

    @Test
    fun testWhenFailure() {
        val t = Try.failure<Int>(java.lang.IllegalArgumentException())

        val u = when (t) {
            is Try.Success -> t.get()
            is Try.Failure -> 0
        }

        assertThat(u, equalTo(0))
    }

    @Test
    fun testOfMap() {

        val mapper: (A) -> C = { a -> C(a.x) }

        val t: Try<B> = Try.success(B(1))
        val u: Try<A> = t.map(mapper)

        assertThat(u.get().x, equalTo(1))
    }

    @Test
    fun testOfMapFailure() {

        val t = Try.failure<Int>(IllegalArgumentException())
        val u = t.map { it }

        assertThat(u.getException(), instanceOf(IllegalArgumentException::class.java))
    }

    @Test
    fun testOfMapThrows() {

        val t = Try.success(1)
        val u = t.map { throw IllegalArgumentException() }

        assertThat(u.getException(), instanceOf(IllegalArgumentException::class.java))
    }

    @Test
    fun testOfFlatMap() {

        val mapper: (A) -> Try<C> = { a -> Try.success(C(a.x)) }

        val t: Try<B> = Try.success(B(1))
        val u: Try<A> = t.flatMap(mapper)

        assertThat(u.get().x, equalTo(1))
    }

    @Test
    fun testOfFilter() {

        val t: Try<B> = Try.success(B(1))
        val filter: (A) -> Boolean = { true }

        val u: Try<B> = t.filter(filter)

        assertThat(u.get().x, equalTo(1))
    }

    @Test
    fun testOfRecover() {

        val t: Try<B> = Try.failure(NoSuchElementException())
        val u = t.recover { C(2) }

        assertThat(u.get().x, equalTo(2))
    }

    @Test
    fun testOfRecoverClass() {

        val t: Try<B> = Try.failure(NullPointerException("foo"))
        val u = t.recover(RuntimeException::class) { e -> C(e.message?.length ?: 0) }

        assertThat(u.get().x, equalTo(3))
    }

    @Test
    fun testOfRecoverWith() {

        val t: Try<B> = Try.failure(NoSuchElementException())
        val u = t.recoverWith { Try.success(C(2)) }

        assertThat(u.get().x, equalTo(2))
    }

    @Test
    fun testOfRecoverWithClass() {

        val t: Try<B> = Try.failure(NullPointerException("foo"))
        val u = t.recoverWith(RuntimeException::class) { e -> Try.success(C(e.message?.length ?: 0)) }

        assertThat(u.get().x, equalTo(3))
    }



}