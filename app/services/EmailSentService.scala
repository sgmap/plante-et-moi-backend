package services

import javax.inject.Inject

import models.Email
import play.api.db.DBApi
import anorm._
import anorm.JodaParameterMetaData._

@javax.inject.Singleton
class EmailSentService @Inject()(dbapi: DBApi) {
  private val db = dbapi.database("default")
  val simple: RowParser[Email] = Macro.parser[Email]("id", "application_id", "agent_id", "city", "creation_date", "type", "subject", "sent_from", "sent_to", "body_text", "reply_to")

  def insert(email: Email) = db.withConnection { implicit connection =>
    SQL(
      """
          INSERT INTO email_sent VALUES (
            {id}, {application_id}, {agent_id}, {city}, {creation_date}, {type}, {subject}, {sent_from}, {sent_to}, {body_text}, {reply_to}
          )
      """
    ).on(
      'id -> email.id,
      'application_id -> email.applicationId,
      'agent_id -> email.agentId,
      'city -> email.city,
      'creation_date -> email.creationDate,
      'type -> email._type,
      'subject -> email.subject,
      'sent_from -> email.sentFrom,
      'sent_to -> email.sentTo,
      'body_text -> email.bodyText,
      'reply_to -> email.replyTo
    ).executeUpdate()
  }

  def findByApplicationId(applicationId: String) = db.withConnection { implicit connection =>
    SQL"""SELECT * FROM email_sent WHERE application_id = $applicationId ORDER BY creation_date ASC""".as(simple.*)
  }

  def findBySentTo(sentToEmail: String, emailType: String) = db.withConnection { implicit connection =>
    val regex = s"%<$sentToEmail>%"
    SQL"""SELECT * FROM email_sent WHERE array_to_string(sent_to, ',') like $regex AND type = $emailType""".as(simple.*)
  }
}