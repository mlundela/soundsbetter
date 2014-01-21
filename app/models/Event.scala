package models

import java.util.Date

case class Event(date: Date,
                 name: String,
                 venue: String,
                 soundcloud: Option[String] = None,
                 spotify: Option[String] = None)
