package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Logger
import play.api.Play.current
import utilities.{JSONSerializable, Generators}
import play.api.libs.json.{JsValue, Json}

/**
 * A model for keeping an account
 *
 * @param id        Id of the account which is an auto incremented number
 * @param email     Email address of the account
 * @param password  Password of the account
 */
case class Account(id: Long, email: String, password: String) extends PeoplemeterModel

/**
 * Companion object of [[models.Account]] acting as data access layer
 */
object Account {
  /**
   * A result set parser for account records in database, maps records to a [[models.Account]] object
   */
  val accountParser = {
    get[Long]("id") ~ get[String]("email") ~ get[String]("password") map {
      case id ~ email ~ password => Account(id, email, password)
    }
  }

  /**
   * Creates an account with given information and inserts it to the database
   *
   * @param email       Email address of the account
   * @param rawPassword Raw password of the account that will be hashed
   *
   * @return            An optional [[models.Account]] if successful, None if any error occurs
   */
  def create(email: String, rawPassword: String): Option[Account] = {
    try {
      val password = Generators.generateSHA512(rawPassword)

      DB.withConnection { implicit c =>
        val insertResult: Option[Long] = SQL(
          """insert into accounts (email, password)
             values ({email}, {password})""")
          .on("email" -> email, "password" -> password).executeInsert()
        if(!insertResult.isEmpty)
          Option(Account(insertResult.get, email, password))
        else {
          Logger.error(s"Account.create() - Account creation failed for $email, cannot insert!")
          None
        }
      }
    }
    catch {
      case e: Exception =>
        Logger.error(s"Account.create() - Account creation failed for $email, ${e.getMessage}")
        None
    }
  }

  /**
   * Reads an account by given id from the database
   *
   * @param id  Id of the account
   *
   * @return    An optional [[models.Account]] if successful, None if any error occurs
   */
  def read(id: Long): Option[Account] = {
    try {
      DB.withConnection { implicit c =>
        SQL(
          """select id, email, password from accounts
             where id={id} limit 1""").on("id" -> id).as(accountParser singleOpt)
      }
    }
    catch {
      case e: Exception =>
        Logger.error(s"Account.read() - Account reading failed for $id, ${e.getMessage}")
        None
    }
  }

  /**
   * Reads an account by given id from the database
   *
   * @param email Email address of the account
   *
   * @return      An optional [[models.Account]] if successful, None if any error occurs
   */
  def read(email: String): Option[Account] = {
    try {
      DB.withConnection { implicit c =>
        SQL(
          """select id, email, password from accounts
             where email={email} limit 1""").on("email" -> email)
        .as(accountParser singleOpt)
      }
    }
    catch {
      case e: Exception =>
        Logger.error(s"Account.read() - Account reading failed for $email, ${e.getMessage}")
        None
    }
  }

  /**
   * Deletes an account with given id from the database
   *
   * @param id  Id of the account
   *
   * @return    true if successfully deleted, false if any error occurs
   */
  def delete(id: Long): Boolean = {
    try {
      DB.withConnection { implicit c =>
        SQL("""delete from accounts where id={id}""")
          .on("id" -> id).executeUpdate() > 0
      }
    }
    catch {
      case e: Exception =>
        Logger.error(s"Account.delete() - Account deleting failed for $id, ${e.getMessage}")
        false
    }
  }

  implicit object AccountAsJSON extends JSONSerializable[Account]
  {
    def toJSON(account: Account): JsValue = Json.obj(
      "id" -> account.id,
      "email" -> account.email,
      "password" -> account.password
    )

    def fromJSON(json: JsValue): Account =
    {
      ((json \ "id").asOpt[Long], (json \ "email").asOpt[String], (json \ "password").asOpt[String]) match
      {
        case (Some(id: Long), Some(email: String), Some(password: String)) => Account(id, email, password)
        case _ => throw new IllegalArgumentException("Invalid Account JSON!")
      }
    }
  }
}
