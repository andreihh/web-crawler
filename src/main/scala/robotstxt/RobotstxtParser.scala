package robotstxt

import scala.util.Try

/**
 * @author andrei
 */
final class RobotstxtParser(val agentName: String) {
  private def getRulesFromDirectives(content: String): RuleSet = {
    val directiveRegex = RobotstxtParser.directive.r
    val directives = directiveRegex.findAllIn(content).toSeq.flatMap({
      case directiveRegex(directive, subject) => RobotstxtParser.supported
        .filter(directive.matches)
        .map(d => (d, subject))
    }).filter({ case (directive, subject) => subject.nonEmpty })
      .groupBy({ case (directive, subject) => directive })
      .mapValues(_.map({ case (k, v) => v }))
    val allow = directives.getOrElse(RobotstxtParser.allow, Nil)
    val disallow = directives.getOrElse(RobotstxtParser.disallow, Nil)
    val crawlDelay = Try {
      directives(RobotstxtParser.crawlDelay).map(_.toDouble).min
    }
    RuleSet(allow, disallow, crawlDelay.getOrElse(0.0))
  }

  private def getRulesForUserAgent(
      userAgent: String,
      content: String): Option[RuleSet] = RobotstxtParser
    .content(userAgent).r
    .unapplySeq(content)
    .flatMap(g => g.headOption)
    .map(getRulesFromDirectives)

  def getRules(rawContent: String): RuleSet = {
    val content = rawContent.replaceAll(RobotstxtParser.comment, "")
    getRulesForUserAgent(agentName, content)
      .orElse(getRulesForUserAgent("\\*", content))
      .getOrElse(RuleSet.empty)
  }
}

object RobotstxtParser {
  def apply(agentName: String) = new RobotstxtParser(agentName)

  val comment = """[\s ]#.*+"""
  val wildcard = """[\s\S]*"""
  val value = """([\w\Q-.~:/?#[]@!$&'()*+,;=\E]*+)"""
  val userAgent = """[uU][sS][eE][rR]-[aA][gG][eE][nN][tT]"""
  val allow = """[aA][lL][lL][oO][wW]"""
  val disallow = """[dD][iI][sS]""" + allow
  val crawlDelay = """[cC][rR][aA][wW][lL]-[dD][eE][lL][aA][yY]"""
  val supported = Seq(allow, disallow, crawlDelay, "[^: ]*+")
  val directive =
    "(?:\\s(" + supported.mkString("|") + ") *+: *+" + value + " *+)"

  def userAgentDirective(agentName: String): String =
    userAgent + " *+: *+" + agentName + " *+"

  def content(agentName: String): String =
    wildcard +
      userAgentDirective(agentName) + "(" + directive + "*+)" +
      wildcard
}