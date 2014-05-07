package controllers

import play.api.mvc._
import utilities.{SidebarItems, Authenticated}
import play.api.data.Form
import play.api.data.Forms._
import play.api.Logger
import models.{House, Channel}
import play.api.libs.json.JsValue
import play.api.http.MimeTypes

/**
 * Channels controller which controls everything about managing channels
 */
object Channels extends Controller {
  /**
   * A form matcher for the channel form, maps the form data to a [[ChannelFormData]] object
   */
  val channelForm: Form[ChannelFormData] = Form(
    mapping(
      "name" -> nonEmptyText,
      "logoPosition" -> number(1, 4)
    )(ChannelFormData.apply)(ChannelFormData.unapply)
  )

  def renderPage = Authenticated { implicit request =>
    val channelList: List[Channel] = Channel.read
    Ok(views.html.channels(channels = channelList, context = request.context, sidebarItems = SidebarItems.activate("Channels")))
  }

  def renderEditChannelPage(id: Long) = Authenticated { implicit request =>
    Ok(views.html.channelDetails(channel = Channel.read(id), context = request.context, sidebarItems = SidebarItems.activate("Channels")))
  }

  def renderAddChannelPage = Authenticated { implicit request =>
    Ok(views.html.channelDetails(isAddingChannel = true, context = request.context, sidebarItems = SidebarItems.activate("Channel")))
  }

  def addChannel() = Authenticated { implicit request =>
    channelForm.bindFromRequest().fold(
      errors => {
        Logger.error(s"Channels.addChannel() - Channel adding failed, invalid form data as ${errors.errorsAsJson}!")
        Redirect(routes.Channels.renderPage())
      },
      channelFormData => {
        Channel.create(channelFormData.name, channelFormData.logoPosition).fold({
          Logger.error(s"Channels.addChannel() - Channel adding failed, cannot insert!")
          Redirect(routes.Channels.renderPage())
        })({
          channel: Channel => Redirect(routes.Channels.renderPage())
        })
      }
    )
  }

  def editChannel(id: Long) = Authenticated { implicit request =>
    channelForm.bindFromRequest().fold(
      errors => {
        Logger.error(s"Channels.editChannel() - Channel editing failed for id $id, invalid form data as ${errors.errorsAsJson}!")
        Redirect(routes.Channels.renderPage())
      },
      channelFormData => {
        val result: Boolean = Channel.update(id, channelFormData.name, channelFormData.logoPosition)
        if(!result) {
          Logger.error(s"Channels.editChannel() - Channel editing failed for id $id, cannot update!")
        }
        Redirect(routes.Channels.renderPage())
      }
    )
  }

  def deleteChannel(id: Long) = Authenticated { implicit request =>
    Channel.delete(id)
    Redirect(routes.Channels.renderPage())
  }

  def getChannelList = Action { implicit request =>
    request.body.asJson.fold(BadRequest("Invalid request!"))({ json =>
      (json \ "deviceId").asOpt[String].fold(BadRequest("Invalid JSON!"))({ deviceId =>
        House.read(deviceId).fold(BadRequest("Invalid device id!"))({ house =>
          val channelListJSON: List[JsValue] = Channel.read.map(Channel.ChannelAsJSON.toJSON)
          Ok(channelListJSON.mkString("[",",","]")).as(MimeTypes.JSON)
        })
      })
    })
  }

  def getChannelLogo(id: Long) = Action { implicit request =>
    request.body.asJson.fold(BadRequest("Invalid request!"))({ json =>
      (json \ "deviceId").asOpt[String].fold(BadRequest("Invalid JSON!"))({ deviceId =>
        House.read(deviceId).fold(BadRequest("Invalid device id!"))({ house =>
          Channel.read(id).fold(BadRequest("Invalid channel id!"))({ channel =>
            // TODO: Send logo image
            Ok
          })
        })
      })
    })
  }
}

/**
 * A model of channel form
 *
 * @param name          Name of the channel
 * @param logoPosition  Position of logo of the channel (1: top-left, 2: top-right, 3: bottom-left, 4: bottom-right)
 */
case class ChannelFormData(name: String, logoPosition: Int)