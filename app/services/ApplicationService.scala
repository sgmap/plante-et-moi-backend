package services

import javax.inject.Inject
import anorm.Column._
import anorm.{Macro, MetaDataItem, RowParser, SQL, TypeDoesNotMatch}
import models.{Application, Coordinates}
import play.api.db.DBApi
import play.api.libs.json._
import anorm._
import anorm.JodaParameterMetaData._
import models.Forms.ApplicationEdit

@javax.inject.Singleton
class ApplicationService @Inject()(dbapi: DBApi) extends AnormJson with AnormCoordinate {
  private val db = dbapi.database("default")

  @inline private def className(that: Any): String =
    if (that == null) "<null>" else that.getClass.getName

  private implicit val coordinatesParser: RowParser[Coordinates] = Macro.namedParser[Coordinates]
  private implicit val fieldsMapParser: anorm.Column[Map[String,String]] =
    nonNull { (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      value match {
        case json: org.postgresql.util.PGobject =>
          Right(Json.parse(json.getValue).as[Map[String,String]])
        case json: String =>
          Right(Json.parse(json).as[Map[String,String]])
        case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${className(value)} to Map[String,String] for column $qualified"))
      }
    }
  private implicit val fieldsListParser: anorm.Column[List[String]] =
    nonNull { (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      value match {
        case array: org.postgresql.jdbc.PgArray=>
          Right(array.getArray.asInstanceOf[Array[String]].toList)
        case json: org.postgresql.util.PGobject =>
          Right(Json.parse(json.getValue).as[List[String]])
        case json: String =>
          Right(Json.parse(json).as[List[String]])
        case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${className(value)} to List[String] for column $qualified"))
      }
    }


  private val simple: RowParser[Application] = Macro.parser[Application](
    "id",
    "city",
    "status",
    "application_imported.applicant_firstname",
    "application_imported.applicant_lastname",
    "application_imported.applicant_email",
    "application_imported.applicant_address",
    "type",
    "address",
    "creation_date",
    "coordinates",
    "source",
    "source_id",
    "application_imported.applicant_phone",
    "fields",
    "application_imported.files",
    "application_extra.files",
    "reviewer_agent_ids",
    "decision_sended_date",
    "application_extra.applicant_firstname",
    "application_extra.applicant_lastname",
    "application_extra.applicant_email",
    "application_extra.applicant_address",
    "application_extra.applicant_phone"
  )

  def findByApplicationId(applicationId: String) = db.withConnection { implicit connection =>
    SQL("SELECT * FROM application_imported INNER JOIN application_extra ON (application_imported.id = application_extra.application_id) WHERE id = {id} ").on('id -> applicationId).as(simple.singleOpt)
  }

  def findByApplicationIds(ids: List[String]) = db.withConnection { implicit connection =>
    SQL"""SELECT * FROM application_imported INNER JOIN application_extra ON (application_imported.id = application_extra.application_id) WHERE ARRAY[$ids]::uuid[] @> ARRAY[id]::uuid[]""".as(simple.*)
  }

  def insert(application: Application) = db.withTransaction { implicit connection =>
    SQL(
      """
          INSERT INTO application_imported VALUES (
            {id}, {city}, {applicant_firstname}, {applicant_lastname}, {applicant_email}, {applicant_address}, {type}, {address}, {creation_date}, point({latitude}, {longitude}), {source}, {source_id}, {applicant_phone}, {fields},{files}
          )
      """
    ).on(
      'id -> application.id,
      'city -> application.city,
      'applicant_firstname -> application.applicantFirstname,
      'applicant_lastname -> application.applicantLastname,
      'applicant_email -> application.applicantEmail,
      'applicant_address -> application.applicantAddress,
      'type -> application._type,
      'address -> application.address,
      'creation_date -> application.creationDate,
      'latitude -> application.coordinates.latitude,
      'longitude -> application.coordinates.longitude,
      'source -> application.source,
      'source_id -> application.sourceId,
      'applicant_phone -> application.applicantPhone,
      'fields -> Json.toJson(application.fields),
      'files -> Json.toJson(application.files)
    ).executeUpdate()
    SQL(
      """
          INSERT INTO application_extra VALUES (
            {application_id}, {status}
          ) ON CONFLICT DO NOTHING
      """
    ).on(
      'application_id -> application.id,
      'status -> application.status
    ).executeUpdate()
  }
  def findByCity(city: String, orderedDescending: Boolean = true) = db.withConnection { implicit connection =>
    val orderString = orderedDescending match {
      case true => "DESC"
      case false => "ASC"
    }
    SQL(s"SELECT * FROM application_imported INNER JOIN application_extra ON (application_imported.id = application_extra.application_id) WHERE city = {city} ORDER BY creation_date $orderString").on('city -> city).as(simple.*)
  }

  def update(application: Application) = db.withConnection { implicit connection =>
    SQL"""UPDATE application_extra SET
            status = ${application.status},
            reviewer_agent_ids = array[${application.reviewerAgentIds}]::text[],
            decision_sended_date = ${application.decisionSendedDate}
          WHERE application_id = ${application.id}
    """.executeUpdate()
  }


  def update(id: String, applicationEdit: ApplicationEdit) = db.withConnection { implicit connection =>
    SQL"""UPDATE application_extra SET
            applicant_email = ${applicationEdit.applicantEmail},
            applicant_firstname = ${applicationEdit.applicantFirstname},
            applicant_lastname = ${applicationEdit.applicantLastname},
            applicant_address = ${applicationEdit.applicantAddress},
            applicant_phone = ${applicationEdit.applicantPhone}
          WHERE application_id = ${id}
    """.executeUpdate() == 1
  }

  def updateFixImport(application: Application) = db.withConnection { implicit connection =>
    SQL"""UPDATE application_imported SET
          fields = ${Json.toJson(application.fields)}
          WHERE id = ${application.id}
    """.executeUpdate()
  }
}