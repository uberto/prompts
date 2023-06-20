
# KondorJson

A functional Kotlin library to serialize/deserialize Json fast and safely without using reflection, annotations or code generation.

Kondor is based on the concept of a JsonConverter, which is a class that maps each domain class to its Json representation. The converter defines how the Json would look like for each of the types you want to persist, using a high level DSL.

No need of custom Data Transfer Objects, and custom serializer, no matter how complex is the Json format you need to use. You can also define more than one converter for each class if you want to have multiple formats for the same types, for example in case of versioned api or different formats for Json in HTTP and persistence.

## Dependency declaration

Gradle

```groovy
implementation 'com.ubertob.kondor:kondor-core:1.8.2'
```

## How It Works

To use Kondor we need to define a Converter for each type (or class of types).

Let's analyze an example in details:

```kotlin
data class Product(val id: Int, val shortDesc: String, val longDesc: List<String>, val price: Double?) // 1

object JProduct : JAny<Product>() { // 2

   val id by num(Product::id) // 3
   val long_description by array(Product::longDesc) // 4
   val `short-desc` by str(Product::shortDesc) // 5
   val price by num(Product::price) // 6

   override fun JsonNodeObject.deserializeOrThrow() = // 7
      Product( // 8
         id = +id, //9 
         shortDesc = +`short-desc`,
         longDesc = +long_description,
         price = +price
      )
}
```

1. This is the class we want to serialize/deserialize
2. Here we define the converter, inheriting from a `JAny<T>` where `T` is our type. If we want to serialize a collection we can start from `JList` or `JSet` and so on, we can also create new abstract converters.
3. Inside the converter we need to define the fields as they will be saved in Json. For each field we need to specify the getter for the serialization, inside a function that represent the kind of Json node (boolean, number, string,array, object) and the specific converter needed for its type. If the converter or the getter is not correct it won't compile.
4. The name of the field is taken from the variable name, `long_description` in this case. Array will use a JSON array
5. Using ticks we can also use names illegal for variables in Kotlin
6. Nullable/optional fields are handled automatically.
7. We then need to define the method to create our objects from Json fields. If we are only interested in serialization we can leave the method empty.
8. Here we use the class constructor, but we could have used any function that return a `Product`
9. To get the value from the fields we use the `unaryplus` operator. It is easy to spot any mistake since we match the name of parameter with the fields.

## Avoid Exceptions

When failing to parse a Json, Kondor is not throwing any exception, instead `fromJson` and `fromJsonNode` methods return
an `Outcome<T>` instead of a simple `T`. 

`Outcome` is an example of the *Either* monad specialized for error handling patterns, this is how handle errors:


```kotlin
val htmlPage = JCustomer.parseJson(jsonString)
   .transform { customer ->
      display(customer)
   }.recover { error ->
      display(error)
   }
```

using `transform` we can convert the `Outcome<Customer>` to something else, for example a `Outcome<HtmlPage>`, then using `recover` we can convert the error result to the same type and remove the `Outcome`.

## Special Cases

With Kondor is easy to solve all Json mappings, including not so trivial ones. For example:

### Enums

Enums are automatically transformed in strings. For example with these types:

```kotlin
enum class TaxType { Domestic, Exempt, EU, US, Other }

data class Company(val name: String, val taxType: TaxType)
```

You can create this converter:

```kotlin
object JCompany : JAny<Company>() {

   private val name by str(Company::name)
   private val tax_type by str(Company::taxType)

   override fun JsonNodeObject.deserializeOrThrow() =
      Company(
         name = +name,
         taxType = +tax_type
      )
}
```

And it will be mapped to this Json format:

```json
{
   "name": "Company Name",
   "tax_type": "Domestic"
}
```

### Sealed classes and polymorphic Json

To store in Json a sealed class, or an interface with a number of known implementations you can use the `JSealed` base converter.

For example assuming `Customer` can be either a `Person` or a `Company`:

```kotlin
sealed class Customer()
data class Person(val id: Int, val name: String) : Customer()
data class Company(val name: String, val taxType: TaxType) : Customer()
```

You just need to map each converter to a string and (optionally) specifiy the name of the discriminator field:

```kotlin
object JCustomer : JSealed<Customer> {

    override val discriminatorFieldName = "type"
   
    override val subtypesJObject: Map<String, JObject<out Customer>> =
        mapOf(
            "private" to JPerson,
            "company" to JCompany
        )

   override fun extractTypeName(obj: Customer): String =
      when (obj) {
         is Person -> "private"
         is Company -> "company"
      }
}
```

It will be mapped in a Json like this:

```json
{
   "type": "private",
   "id": 1,
   "name": "ann"
}
```

Where "type" here is the discriminator field.

### Flatten Fields

Let's say we have a class `FileInfo` that maps to this json format:

```json
{
   "name": "filename",
   "date": 0,
   "size": 123,
   "folderPath": "/"
}
```

Now we need to create a type which is same as `FileInfo` but with a boolean field added:

```kotlin
data class SelectedFile(val selected: Boolean, val file: FileInfo)
```


We want to flatten the fields together:

```json
{
   "selected": true,
   "name": "filename",
   "date": 0,
   "size": 123,
   "folderPath": "/"
}
```

With Kondor is easy to do this using the `flatten` format:

```kotlin
object JSelectedFile : JAny<SelectedFile>() {

   val selected by bool(SelectedFile::selected)
   val file_info by flatten(JFileInfo, SelectedFile::file)

   override fun JsonNodeObject.deserializeOrThrow() =
      SelectedFile(
         +selected,
         +file_info
      )

}
```

Note that it only works with non-nullable fields and it requires that there are no fields with same name on `SelectedFile` and `FileInfo`.

### Storing a Map as Json

Let's say you have a field which is a map and you want to save it as a Json object.

For example a map of things to do, with a short key and a longer description:

```kotlin
data class Notes(val updated: Instant, val thingsToDo: Map<String, String>)
```

You just need to use the `JMap` converter and passing it the converter for the value type of the Map (the keys have to be `String` because of Json syntax):

```kotlin
object JNotes : JAny<Notes>() {
   private val updated by str(Notes::updated, JInstantD)
   private val things_to_do by obj(JMap(JString), Notes::thingsToDo)

   override fun JsonNodeObject.deserializeOrThrow() =
      Notes(
         updated = +updated,
         thingsToDo = +things_to_do
      )
}
```

The result will be a Json like this:

```json
{
   "updated": "2021-03-26T19:19:20.093501Z",
   "things_to_do": {
      "something": "lorem ipsum",
      "something else": "Lorem ipsum dolor sit amet",
      "another thing to do": "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
      "ditto": "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididun"
   }
}
```

