import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Publisher
import java.net.URL
import java.util.concurrent.*


object NetUtil {

    fun get(url: String): String {
        val openConnection = URL(url).openConnection()
        return openConnection.getInputStream().bufferedReader().readText()
    }
}

fun 打印线程(tag: String, action: String) {
    println("[$tag] $action 运行的线程为:${Thread.currentThread().name}")
}

fun 打印结果(tag: String, result: String) {
    println("[$tag] 结果 :$result")
}

/**
 * 使用子线程请求接口
 */

fun 基本多线程方式() {
    val tag = "基本多线程方式"
    val futureTask = FutureTask(Callable<String> {
        打印线程(tag, "进行网络请求")
        NetUtil.get("http://api.douban.com/v2/movie/top250")
    })
    Thread(futureTask).start()
    val result = futureTask.get()
    打印线程(tag, "获得结果")
    打印结果(tag, result)
}

fun 安卓handler的方式() {

}

fun rxjava的方式() {
    val tag = "rxjava的方式"

    // 阻塞队列
    val blockingDeque = LinkedBlockingDeque<Runnable>()

    val disposable = Flowable
            .defer {
                打印线程(tag, "进行网络请求")
                Flowable.just(NetUtil.get("http://api.douban.com/v2/movie/top250"))
            }
            .subscribeOn(Schedulers.newThread())
            .observeOn(Schedulers.from {
                // 把任务添加到阻塞队列来换线程
                blockingDeque.add(it)
            })
            .subscribe {
                打印线程(tag, "获得结果")
                打印结果(tag, it)
            }
    while (!disposable.isDisposed) {
        blockingDeque.forEach { it.run() }
        TimeUnit.MILLISECONDS.sleep(10)
    }
}

fun 协程的方式() {
    suspend fun get() {
        NetUtil.get("http://api.douban.com/v2/movie/top250")
    }



}

fun main(args: Array<String>) {
    基本多线程方式()
    rxjava的方式()
}