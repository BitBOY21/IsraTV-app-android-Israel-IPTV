package com.isratv.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isratv.android.domain.model.UpdateInfo
import com.isratv.android.domain.usecase.CheckUpdateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val checkUpdateUseCase: CheckUpdateUseCase
) : ViewModel() {

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog

    // companion object הופך את המשתנה לסטטי - הוא ישרוד כל עוד האפליקציה בזיכרון
    companion object {
        private var hasCheckedInThisSession = false
    }

    init {
        checkForUpdate()
    }

    private fun checkForUpdate() {
        // אם המשתנה הסטטי אומר שכבר בדקנו - אנחנו עוצרים כאן
        if (hasCheckedInThisSession) return

        viewModelScope.launch {
            val update = checkUpdateUseCase.invoke()
            if (update != null && update.hasUpdate) {
                _updateInfo.value = update
                _showDialog.value = true
            }
            // ברגע שסיימנו את הבדיקה (גם אם אין עדכון), נסמן שבדקנו
            hasCheckedInThisSession = true
        }
    }

    fun dismissDialog() {
        _showDialog.value = false
    }
}