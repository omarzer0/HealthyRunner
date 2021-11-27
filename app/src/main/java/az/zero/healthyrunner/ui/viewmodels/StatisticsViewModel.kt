package az.zero.healthyrunner.ui.viewmodels

import androidx.lifecycle.ViewModel
import az.zero.healthyrunner.repositories.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    val mainRepository: MainRepository
) : ViewModel() {


}