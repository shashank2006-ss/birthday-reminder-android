package com.birthdayreminder.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.birthdayreminder.app.data.model.Person
import com.birthdayreminder.app.data.repository.BirthdayRepository
import com.birthdayreminder.app.service.AlarmScheduler
import com.birthdayreminder.app.service.MessagingHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class BirthdayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BirthdayRepository.getInstance(application)
    val allPeople: StateFlow<List<Person>> = repository.peopleFlow

    val searchQuery = MutableStateFlow("")

    val filteredPeople: StateFlow<List<Person>> = combine(allPeople, searchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.phoneNumber.contains(query, ignoreCase = true) ||
                it.getFormattedDate().contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val editingPerson = MutableStateFlow<Person?>(null)
    val quickSendPerson = MutableStateFlow<Person?>(null)

    fun savePerson(person: Person) {
        val context = getApplication<Application>()
        repository.savePerson(person)
        AlarmScheduler.scheduleBirthdayAlarm(context, person)
    }

    fun deletePerson(id: String) {
        val context = getApplication<Application>()
        val person = repository.getById(id)
        if (person != null) {
            AlarmScheduler.cancelBirthdayAlarm(context, person)
        }
        repository.deletePerson(id)
    }

    fun toggleEnabled(person: Person) {
        val updated = person.copy(isEnabled = !person.isEnabled)
        savePerson(updated)
    }

    fun sendWish(person: Person, messageOverride: String? = null) {
        val context = getApplication<Application>()
        val success = MessagingHelper.triggerSend(context, person, messageOverride)
        if (success) {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            repository.updateLastSentYear(person.id, currentYear)
        }
    }

    fun getPeopleForMonthAndDay(month: Int, day: Int): List<Person> {
        return repository.getPeopleForDate(month, day)
    }
}
