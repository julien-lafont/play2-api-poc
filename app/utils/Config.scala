package utils

import scala.collection.JavaConverters.iterableAsScalaIterableConverter

object Config {

  private lazy val conf = play.api.Play.current.configuration

  def getString(key: String): String = conf.getString(key).getOrElse(throwError(key))
  def getInt(key: String) = conf.getInt(key).getOrElse(throwError(key))
  def getLong(key: String) = conf.getLong(key).getOrElse(throwError(key))
  def getBoolean(key: String) = conf.getBoolean(key).getOrElse(throwError(key))
  def getConfig(key: String) = conf.getConfig(key).getOrElse(throwError(key))
  def getDouble(key: String) = conf.getDouble(key).getOrElse(throwError(key))
  def getObject(key: String) = conf.getObject(key).getOrElse(throwError(key))

  def getStringList(key: String): List[String] = conf.getStringList(key).getOrElse(throwError(key)).asScala.toList
  def getIntList(key: String) = conf.getIntList(key).getOrElse(throwError(key)).asScala.toList
  def getLongList(key: String) = conf.getLongList(key).getOrElse(throwError(key)).asScala.toList
  def getBooleanList(key: String) = conf.getBooleanList(key).getOrElse(throwError(key)).asScala.toList
  def getConfigList(key: String) = conf.getConfigList(key).getOrElse(throwError(key)).asScala.toList
  def getDoubleList(key: String) = conf.getDoubleList(key).getOrElse(throwError(key)).asScala.toList
  def getObjectList(key: String) = conf.getObjectList(key).getOrElse(throwError(key)).asScala.toList

  private def throwError(key: String) = throw conf.globalError(s"Missing configuration key [$key]")
}
