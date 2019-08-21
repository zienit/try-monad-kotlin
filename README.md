# The Try monad for Kotlin

Writing Kotlin code in functional programming style is cool. 
However, dealing with exceptions feels somewhat awkward. 
A given function can return a result, as specified in its signature, but can also throw an exception, which is not specified in the signature.

The Try monad allows you to specify that a function will return a result of a certain type when all goes well, but will return (instead of throw) an exception if something goes wrong.
Thereby, the function signature warns you about possible troubles and urges you to deal with them.

Note that I will not explain what a monad is. This is not needed to use Try effectively. 
Once you see the similarities between this monad and other ones like List, you'll understand what a monad is.

In this project a very small but effective implementation of the Try monad is provided.
Let's go through some examples to see how Try can be used.

First, we have a tiny 'database' of programming languages.

```
data class ProgrammingLanguage(val name: String, val creator: String, val predecessor: String?)

val databaseOfLanguages = listOf(
    ProgrammingLanguage("C", "Dennis Ritchie", null),
    ProgrammingLanguage("Java", "James Gosling", "C"),
    ProgrammingLanguage("Kotlin", "JetBrains", "Java")
)
```

Next, there's a function to find a language by its name. 
This function may fail for various reasons, one being that a language with the provided name may not exists. 
The fact that the function may fail is expressed in the function signature:

```
fun findLanguage(query: String): Try<ProgrammingLanguage> = databaseOfLanguages
    .filter { it.name == query }
    .map { it.success() }
    .firstOrNull() ?: NoSuchElementException().failure()
```

Inside the function some specific things happen. 
If a programming language is found, it is wrapped inside an instance of (inner) class Try.Success.
The convenience extension function .succes() does just that. If we do not find a language, we create an instance of NoSuchElementException and wrap it inside an instance of Try.Failure.

Lets write a little test for findLanguage:

```
@Test
fun testFindLanguage() {
    val kotlin = findLanguage("Kotlin")
    val foobar = findLanguage("Foobar")

    assertThat(kotlin.isSuccess(), equalTo(true))
    assertThat(kotlin.get().name, equalTo("Kotlin"))
    
    assertThat(foobar.isSuccess(), equalTo(false))
    assertThat(foobar.getException(), instanceOf(NoSuchElementException::class.java))
}
```
The variable kotlin holds the result of a successful call to findLanguage. The wrapped language can be accessed though method Try.get().
The variable foobar holds a failure. The wrapped exception is accessible via method getException(). Note that calling .get() on a failure and .getException() on a success will throw an exception.

For the next example we introduce the following function. This higher-order function creates a function that lists the name of all languages in our database. In fact, it can create two versions of such a function; one that is reliable and returns the desired result, and one that is unreliable and fails with a SQLException (imagine our demo database is a real-life RDBMS).

```
fun makeLanguageLister(reliable: Boolean): () -> Try<List<String>> = {
    if (reliable)
        databaseOfLanguages.map { it.name }.toList().success()
    else
        SQLException().failure()
}
```

So lets get on with an example. Here we make use of the workhorse of any monad, the map method. 
The map method executes a provided lambda function if and only if the Try is a success.

```
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
```

Here is another version of the last example. This time we have an unreliable lister. 
Because the Try is a failure, when .get() is invoked, instead of providing a value which it doesn't have, it throws the exception it holds instead.

```
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
```

So how can we deal with failures? How can we turn a failure into a success? Enter the .recover() method:

```
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
```

Now that we've seen the .map() method, lets have a look at .flatMap(). We need this method when the lambda that we need to call returns a Try itself.
We don't want a Try wrapped in a Try. We want .flatMap() to 'flatten' this out. Notice that .flatMap() on List does the same thing, but while dealing with a completely different beast?
Yes? Now you understand what a monad is.

```
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
```

The final example is a bit more involved, because it uses both the Try and the List monad together. 
Lets go through the example line by line:
The call to listLanguage returns a `Try<List<ProgrammingLanguage>>`. 
This is fed into a .flatMap() because the lambda function will yield a Try, as we will see. 
The lambda is fed with a `<List<ProgrammingLanguage>>` as extraced from the Try. 
The entries in the list are all mapped to a `Try<ProgrammingLanguage>` 
and immediately after that the Try is mapped to a `Try<String>`. 
So, we now have a list of tries, which is not very useful in most situations. 
Fortunately, there is an extension function `<List<Try<T>>.toTryList()` provided that turns a list of tries into a try of a list, which is exactly what we need.

```
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
```