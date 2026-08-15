package com.familyos.core.locale

import java.util.Locale

/**
 * Resolves display strings for domain enums based on the active app locale.
 */
object LocalizedLabels {

    private fun lang(): String = Locale.getDefault().language.lowercase()

    fun shoppingCategory(key: String): String = when (lang()) {
        "ru" -> when (key) {
            "PRODUCTS" -> "Продукты"
            "HOME" -> "Дом"
            "PHARMACY" -> "Аптека"
            "AUTO" -> "Авто"
            "PETS" -> "Животные"
            "KIDS" -> "Дети"
            "CLOTHING" -> "Одежда"
            "ELECTRONICS" -> "Электроника"
            else -> "Другое"
        }
        "sr" -> when (key) {
            "PRODUCTS" -> "Namirnice"
            "HOME" -> "Kuća"
            "PHARMACY" -> "Apoteka"
            "AUTO" -> "Auto"
            "PETS" -> "Ljubimci"
            "KIDS" -> "Deca"
            "CLOTHING" -> "Odeća"
            "ELECTRONICS" -> "Elektronika"
            else -> "Ostalo"
        }
        else -> when (key) {
            "PRODUCTS" -> "Products"
            "HOME" -> "Home"
            "PHARMACY" -> "Pharmacy"
            "AUTO" -> "Auto"
            "PETS" -> "Pets"
            "KIDS" -> "Kids"
            "CLOTHING" -> "Clothing"
            "ELECTRONICS" -> "Electronics"
            else -> "Other"
        }
    }

    fun shoppingStatus(key: String): String = when (lang()) {
        "ru" -> when (key) {
            "ACTIVE" -> "Активно"
            "PURCHASED" -> "Куплено"
            "ARCHIVED" -> "Архив"
            else -> key
        }
        "sr" -> when (key) {
            "ACTIVE" -> "Aktivno"
            "PURCHASED" -> "Kupljeno"
            "ARCHIVED" -> "Arhiva"
            else -> key
        }
        else -> when (key) {
            "ACTIVE" -> "Active"
            "PURCHASED" -> "Purchased"
            "ARCHIVED" -> "Archived"
            else -> key
        }
    }

    fun shoppingSort(key: String): String = when (lang()) {
        "ru" -> when (key) {
            "NAME_ASC" -> "Имя А–Я"
            "NAME_DESC" -> "Имя Я–А"
            "NEWEST" -> "Сначала новые"
            "OLDEST" -> "Сначала старые"
            "PRICE_ASC" -> "Цена ↑"
            "PRICE_DESC" -> "Цена ↓"
            "CATEGORY" -> "Категория"
            else -> key
        }
        "sr" -> when (key) {
            "NAME_ASC" -> "Ime A–Š"
            "NAME_DESC" -> "Ime Š–A"
            "NEWEST" -> "Najnovije"
            "OLDEST" -> "Najstarije"
            "PRICE_ASC" -> "Cena ↑"
            "PRICE_DESC" -> "Cena ↓"
            "CATEGORY" -> "Kategorija"
            else -> key
        }
        else -> when (key) {
            "NAME_ASC" -> "Name A–Z"
            "NAME_DESC" -> "Name Z–A"
            "NEWEST" -> "Newest"
            "OLDEST" -> "Oldest"
            "PRICE_ASC" -> "Price ↑"
            "PRICE_DESC" -> "Price ↓"
            "CATEGORY" -> "Category"
            else -> key
        }
    }

    fun taskStatus(key: String): String = when (lang()) {
        "ru" -> when (key) {
            "NEW", "ALL" -> if (key == "ALL") "Все" else "Новая"
            "IN_PROGRESS" -> "В работе"
            "WAITING" -> "Ожидает"
            "DONE" -> "Выполнена"
            "CANCELLED" -> "Отменена"
            "OVERDUE" -> "Просрочена"
            else -> key
        }
        "sr" -> when (key) {
            "ALL" -> "Sve"
            "NEW" -> "Nova"
            "IN_PROGRESS" -> "U toku"
            "WAITING" -> "Na čekanju"
            "DONE" -> "Završena"
            "CANCELLED" -> "Otkazana"
            "OVERDUE" -> "Kasni"
            else -> key
        }
        else -> when (key) {
            "ALL" -> "All"
            "NEW" -> "New"
            "IN_PROGRESS" -> "In progress"
            "WAITING" -> "Waiting"
            "DONE" -> "Done"
            "CANCELLED" -> "Cancelled"
            "OVERDUE" -> "Overdue"
            else -> key
        }
    }

    fun taskPriority(key: String): String = when (lang()) {
        "ru" -> when (key) {
            "LOW" -> "Низкий"
            "MEDIUM" -> "Средний"
            "HIGH" -> "Высокий"
            "URGENT" -> "Срочный"
            else -> key
        }
        "sr" -> when (key) {
            "LOW" -> "Nizak"
            "MEDIUM" -> "Srednji"
            "HIGH" -> "Visok"
            "URGENT" -> "Hitno"
            else -> key
        }
        else -> when (key) {
            "LOW" -> "Low"
            "MEDIUM" -> "Medium"
            "HIGH" -> "High"
            "URGENT" -> "Urgent"
            else -> key
        }
    }

    fun recurrence(key: String): String = when (lang()) {
        "ru" -> when (key) {
            "DAILY" -> "Каждый день"
            "WEEKLY" -> "Каждую неделю"
            "MONTHLY" -> "Каждый месяц"
            "YEARLY" -> "Каждый год"
            else -> key
        }
        "sr" -> when (key) {
            "DAILY" -> "Svaki dan"
            "WEEKLY" -> "Svake nedelje"
            "MONTHLY" -> "Svaki mesec"
            "YEARLY" -> "Svake godine"
            else -> key
        }
        else -> when (key) {
            "DAILY" -> "Daily"
            "WEEKLY" -> "Weekly"
            "MONTHLY" -> "Monthly"
            "YEARLY" -> "Yearly"
            else -> key
        }
    }

    fun eventType(key: String): String = when (lang()) {
        "ru" -> when (key) {
            "BIRTHDAY" -> "День рождения"
            "HOLIDAY" -> "Праздник"
            "MEETING" -> "Встреча"
            "TRIP" -> "Поездка"
            "SCHOOL" -> "Школа"
            "VET" -> "Ветеринар"
            "DOCTOR" -> "Врач"
            "BILL_PAYMENT" -> "Оплата счетов"
            else -> "Другое"
        }
        "sr" -> when (key) {
            "BIRTHDAY" -> "Rođendan"
            "HOLIDAY" -> "Praznik"
            "MEETING" -> "Sastanak"
            "TRIP" -> "Putovanje"
            "SCHOOL" -> "Škola"
            "VET" -> "Veterinar"
            "DOCTOR" -> "Doktor"
            "BILL_PAYMENT" -> "Plaćanje računa"
            else -> "Ostalo"
        }
        else -> when (key) {
            "BIRTHDAY" -> "Birthday"
            "HOLIDAY" -> "Holiday"
            "MEETING" -> "Meeting"
            "TRIP" -> "Trip"
            "SCHOOL" -> "School"
            "VET" -> "Vet"
            "DOCTOR" -> "Doctor"
            "BILL_PAYMENT" -> "Bill payment"
            else -> "Other"
        }
    }

    fun budgetCategory(key: String): String = when (lang()) {
        "ru" -> when (key) {
            "FOOD" -> "Еда"
            "UTILITIES" -> "Коммуналка"
            "CAR" -> "Автомобиль"
            "EDUCATION" -> "Образование"
            "HEALTH" -> "Здоровье"
            "ENTERTAINMENT" -> "Развлечения"
            "TRAVEL" -> "Путешествия"
            else -> "Другое"
        }
        "sr" -> when (key) {
            "FOOD" -> "Hrana"
            "UTILITIES" -> "Komunalije"
            "CAR" -> "Auto"
            "EDUCATION" -> "Obrazovanje"
            "HEALTH" -> "Zdravlje"
            "ENTERTAINMENT" -> "Zabava"
            "TRAVEL" -> "Putovanja"
            else -> "Ostalo"
        }
        else -> when (key) {
            "FOOD" -> "Food"
            "UTILITIES" -> "Utilities"
            "CAR" -> "Car"
            "EDUCATION" -> "Education"
            "HEALTH" -> "Health"
            "ENTERTAINMENT" -> "Entertainment"
            "TRAVEL" -> "Travel"
            else -> "Other"
        }
    }
}
