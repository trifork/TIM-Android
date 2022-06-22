package com.trifork.timencryptedstorage.test

import com.trifork.timencryptedstorage.keyservice.TIMKeyService
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceError
import com.trifork.timencryptedstorage.models.keyservice.response.TIMKeyModel
import com.trifork.timencryptedstorage.models.toTIMSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

class TIMKeyServiceStub(
    val keyId: String = "168dfa8a-a613-488d-876c-1a79122c8d5a",
    val key: String = "/RT5VXFinR27coWdsieCt3UxoKibplkO+bCVNkDJK9o=",
    val longSecret: String = "xe6XhucZ0BnH3yLQFR1wrZgPe3l4q/ymnQCCY/iZs3A=",
    val timKeyModel: TIMKeyModel = TIMKeyModel(keyId, key, longSecret)
) : TIMKeyService {

    override fun createKey(scope: CoroutineScope, secret: String): Deferred<TIMResult<TIMKeyModel, TIMKeyServiceError>> = scope.async {
        return@async timKeyModel.toTIMSuccess()
    }

    override fun getKeyViaSecret(scope: CoroutineScope, secret: String, keyId: String): Deferred<TIMResult<TIMKeyModel, TIMKeyServiceError>> = scope.async {
        return@async timKeyModel.toTIMSuccess()
    }

    override fun getKeyViaLongSecret(scope: CoroutineScope, longSecret: String, keyId: String): Deferred<TIMResult<TIMKeyModel, TIMKeyServiceError>> = scope.async {
        return@async timKeyModel.toTIMSuccess()
    }
}