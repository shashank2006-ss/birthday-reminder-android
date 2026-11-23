package com.birthdayreminder.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.birthdayreminder.app.data.model.Person
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BirthdayRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _peopleFlow = MutableStateFlow<List<Person>>(emptyList())
    val peopleFlow: StateFlow<List<Person>> = _peopleFlow.asStateFlow()

    init {
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val json = prefs.getString(KEY_PEOPLE, null)
        val list = if (json.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val type = object : TypeToken<List<Person>>() {}.type
                gson.fromJson<List<Person>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
        _peopleFlow.value = list.sortedBy { it.getDaysRemaining() }
    }

    private fun saveToPrefs(list: List<Person>) {
        val sorted = list.sortedBy { it.getDaysRemaining() }
        val json = gson.toJson(sorted)
        prefs.edit().putString(KEY_PEOPLE, json).apply()
        _peopleFlow.value = sorted
    }

    @Synchronized
    fun getAll(): List<Person> = _peopleFlow.value

    @Synchronized
    fun getById(id: String): Person? {
        return _peopleFlow.value.find { it.id == id }
    }

    @Synchronized
    fun savePerson(person: Person) {
        val current = _peopleFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == person.id }
        if (index >= 0) {
            current[index] = person
        } else {
            current.add(person)
        }
        saveToPrefs(current)
    }

    @Synchronized
    fun deletePerson(id: String) {
        val current = _peopleFlow.value.toMutableList()
        current.removeAll { it.id == id }
        saveToPrefs(current)
    }

    @Synchronized
    fun getPeopleForDate(month: Int, day: Int): List<Person> {
        return _peopleFlow.value.filter { it.birthMonth == month && it.birthDay == day }
    }

    @Synchronized
    fun updateLastSentYear(personId: String, year: Int) {
        val person = getById(personId) ?: return
        person.lastSentYear = year
        savePerson(person)
    }

    companion object {
        private const val PREFS_NAME = "birthday_reminder_prefs"
        private const val KEY_PEOPLE = "saved_people_list"

        @Volatile
        private var INSTANCE: BirthdayRepository? = null

        fun getInstance(context: Context): BirthdayRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BirthdayRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
