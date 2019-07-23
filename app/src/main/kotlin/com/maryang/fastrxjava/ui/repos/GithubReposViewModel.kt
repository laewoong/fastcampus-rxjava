package com.maryang.fastrxjava.ui.repos

import com.maryang.fastrxjava.data.repository.GithubRepository
import com.maryang.fastrxjava.entity.GithubRepo
import com.maryang.fastrxjava.util.applySchedulersExtension
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class GithubReposViewModel {
    private val repository = GithubRepository()
    private val searchSubject = PublishSubject.create<Pair<String, Boolean>>()
    var searchText = ""

    fun searchGithubRepos(search: String) {
        searchSubject.onNext(search to true)
    }

    fun searchGithubRepos() {
        searchSubject.onNext(searchText to false)
    }

    fun searchGithubReposSubject() =
        searchSubject
            .debounce(400, TimeUnit.MILLISECONDS)
            .doOnNext { searchText = it.first }
            .map { it.second }
            .observeOn(AndroidSchedulers.mainThread())

//    fun searchGithubReposObservable() =
//        Single.create<List<GithubRepo>> { emitter ->
//            repository.searchGithubRepos(searchText)
//                .subscribe({
//                    Completable.merge(
//                        it.map { repo ->
//                            repository.checkStar(repo.owner.userName, repo.name)
//                                .doOnComplete { repo.star = true }
//                                .onErrorComplete()
//                        }
//                    ).subscribe {
//                        emitter.onSuccess(it)
//                    }
//                }, {})
//        }
//            .applySchedulersExtension()
//            .toObservable()

    fun searchGithubReposObservable(): Observable<List<GithubRepo>> {

        fun checkStar(repo: GithubRepo) = repository.checkStar(repo.owner.userName, repo.name)
            .doOnComplete { repo.star = true }
            .onErrorComplete()
            .toSingle { repo }

        return repository.searchGithubRepos(searchText)
            .flatMapObservable { Observable.fromIterable(it) }
            .flatMapSingle { checkStar(it) }
            .toList()
            .applySchedulersExtension()
            .toObservable()
    }

}
