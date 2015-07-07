package fetcher

import dispatch._, Defaults._
import java.net.URL

/**
 * @author andrei
 */
class PageFetcher(userAgent: String, followRedirects: Boolean, timeout: Int) {
  def fetch(urlString: String): Future[Page] = {
    val query = url(urlString).addHeader("User-Agent", userAgent)
    val http = Http.configure {
      _.setFollowRedirects(followRedirects).setConnectionTimeoutInMs(timeout)
    }
    http(query OK as.Bytes).map(content => Page(urlString, content))
  }

  def fetch(url: URL): Future[Page] = fetch(url.toString)
}

object PageFetcher {
  def apply(
      userAgent: String,
      followerRedirects: Boolean = true,
      timeout: Int = 1000): PageFetcher =
    new PageFetcher(userAgent, followerRedirects, timeout)
}