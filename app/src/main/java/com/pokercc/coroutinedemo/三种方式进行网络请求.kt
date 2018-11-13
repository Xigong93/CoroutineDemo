import android.annotation.SuppressLint
import android.os.Handler
import android.os.Message
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import java.net.URL
import java.util.concurrent.*
import kotlinx.coroutines.*
import kotlinx.coroutines.GlobalScope

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

/**
 * 需要通过安卓的单元测试执行
 */
fun 安卓handler的方式() {
    val tag = "基本多线程方式"

    val handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message?) {
        }
    }
    Thread {
        打印线程(tag, "进行网络请求")
        val result = NetUtil.get("http://api.douban.com/v2/movie/top250")
        handler.post {
            打印线程(tag, "获得结果")
            打印结果(tag, result)
        }
    }.start()
    // 避免jvm 退出
    TimeUnit.SECONDS.sleep(2)

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
    val tag = "协程的方式"
    val result = GlobalScope.async {
        打印线程(tag, "进行网络请求")
        NetUtil.get("http://api.douban.com/v2/movie/top250")
    }
    runBlocking {
        打印线程(tag, "获得结果")
        打印结果(tag, result.await())
    }
    // 避免jvm 退出
    TimeUnit.SECONDS.sleep(2)

}

fun main(args: Array<String>) {
    基本多线程方式()
//    安卓handler的方式()
    rxjava的方式()
    协程的方式()
}