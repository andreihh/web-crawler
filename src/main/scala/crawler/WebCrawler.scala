package crawler

import rx.lang.scala.Observable

import fetcher._

import java.net.URL
import java.util.concurrent.atomic.AtomicLong

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

/**
 * @author andrei
 */
object WebCrawler {
  def crawl(configuration: CrawlConfiguration)
           (initial: Traversable[URL]): Observable[(URL, Try[Page])] = {
    Observable[(URL, Try[Page])](subscriber => {
      val frontier = URLFrontier(configuration, initial)
      val fetcher = PageFetcher(
        configuration.userAgentString,
        configuration.followRedirects,
        configuration.connectionTimeoutInMs,
        configuration.requestTimeoutInMs)
      val working = new AtomicLong(0L)
      var crawled = 0L
      while (!subscriber.isUnsubscribed &&
        crawled < configuration.crawlLimit &&
        (working.get > 0L || !frontier.isIdle || !frontier.isEmpty)) {
        if (!frontier.isEmpty) {
          working.incrementAndGet()
          crawled += 1
          val url = frontier.pop()
          val page = url.flatMap(fetcher.fetch)
          page.onComplete(p => {
            subscriber.onNext(Await.result(url, Duration.Inf) -> p)
            p.map(_.outlinks).foreach(_.foreach(frontier.tryPush))
            working.decrementAndGet()
          })
        }
      }
      while (!subscriber.isUnsubscribed && working.get > 0) {}
      subscriber.onCompleted()
    })
  }
}
