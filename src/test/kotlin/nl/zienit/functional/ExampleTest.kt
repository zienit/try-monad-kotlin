package nl.zienit.functional

import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.lang.RuntimeException
import java.sql.SQLException

data class ProgrammingLanguage(val name: String, val creator: String, val predecessor: String?)

val databaseOfLanguages = listOf(
    ProgrammingLanguage("C", "Dennis Ritchie", null),
    ProgrammingLanguage("Java", "James Gosling", "C"),
    ProgrammingLanguage("Kotlin", "JetBrains", "Java")
)

fun findLanguage(query: String): Try<ProgrammingLanguage> = databaseOfLanguages
    .filter { it.name == query }
    .map { it.success() }
    .firstOrNull() ?: NoSuchElementException().failure()

fun makeLanguageLister(reliable: Boolean): () -> Try<List<String>> = {
    if (reliable)
        databaseOfLanguages.map { it.name }.toList().success()
    else
        SQLException().failure()
}

class ExampleTest {

    @Test
    fun testFindLanguage() {
        val kotlin = findLanguage("Kotlin")
        val foobar = findLanguage("Foobar")

        assertThat(kotlin.isSuccess(), equalTo(true))
        assertThat(kotlin.get().name, equalTo("Kotlin"))
        assertThat(foobar.isSuccess(), equalTo(false))
        assertThat(foobar.getException(), instanceOf(NoSuchElementException::class.java))
    }

    @Test
    fun testListLanguages() {

        val listLanguages = makeLanguageLister(reliable = true)

        val languages = listLanguages();

        val greetings = languages
            .map {
                "greetings from ${it.joinToString(",")}"
            }
        assertThat(greetings.get(), equalTo("greetings from C,Java,Kotlin"))
    }

    @Test(expected = SQLException::class)
    fun testListLanguagesFailure() {

        val listLanguages = makeLanguageLister(reliable = false)

        val languages = listLanguages();

        val greetings = languages
            .map {
                "greetings from ${it.joinToString(",")}"
            }
        assertThat(greetings.get(), equalTo("greetings from C,Java,Kotlin"))
    }

    @Test
    fun testListLanguagesRecover() {

        val listLanguages = makeLanguageLister(reliable = false)

        val languages = listLanguages();

        val greetings = languages
            .map {
                "greetings from ${it.joinToString(",")}"
            }
            .recover { "sorry, something went wrong" }
        assertThat(greetings.get(), equalTo("sorry, something went wrong"))
    }

    @Test
    fun testFindPredecessorOfKotlin() {

        val language = findLanguage("Kotlin")
            .flatMap {
                if (it.predecessor == null)
                    NoSuchElementException().failure()
                else
                    findLanguage(it.predecessor)
            }

        assertThat(language.get().name, equalTo("Java"))
    }

    @Test
    fun testListCreators() {

        val listLanguages = makeLanguageLister(reliable = true)

        val creators : Try<List<String>> = listLanguages()
            .flatMap { names ->
                names
                    .map { name ->
                        findLanguage(name)
                            .map { it.creator }
                    }
                    .toTryList()
            }

        assertThat(creators.get(), hasItems("Dennis Ritchie", "James Gosling", "JetBrains"))
    }
}