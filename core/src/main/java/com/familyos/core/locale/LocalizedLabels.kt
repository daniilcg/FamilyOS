package com.familyos.core.locale

import java.util.Locale

/**
 * Resolves display strings for domain enums based on the active app locale.
 */
object LocalizedLabels {

    private fun lang(): String = AppLocale.currentLanguage()

    fun shoppingCategory(key: String): String {
        val map = when (lang()) {
            "ru" -> mapOf(
                "PRODUCTS" to "Продукты", "HOME" to "Дом", "PHARMACY" to "Аптека",
                "AUTO" to "Авто", "PETS" to "Животные", "KIDS" to "Дети",
                "CLOTHING" to "Одежда", "ELECTRONICS" to "Электроника", "OTHER" to "Другое",
            )
            "sr" -> mapOf(
                "PRODUCTS" to "Namirnice", "HOME" to "Kuća", "PHARMACY" to "Apoteka",
                "AUTO" to "Auto", "PETS" to "Ljubimci", "KIDS" to "Deca",
                "CLOTHING" to "Odeća", "ELECTRONICS" to "Elektronika", "OTHER" to "Ostalo",
            )
            "de" -> mapOf(
                "PRODUCTS" to "Lebensmittel", "HOME" to "Haushalt", "PHARMACY" to "Apotheke",
                "AUTO" to "Auto", "PETS" to "Tiere", "KIDS" to "Kinder",
                "CLOTHING" to "Kleidung", "ELECTRONICS" to "Elektronik", "OTHER" to "Sonstiges",
            )
            "fr" -> mapOf(
                "PRODUCTS" to "Courses", "HOME" to "Maison", "PHARMACY" to "Pharmacie",
                "AUTO" to "Auto", "PETS" to "Animaux", "KIDS" to "Enfants",
                "CLOTHING" to "Vêtements", "ELECTRONICS" to "Électronique", "OTHER" to "Autre",
            )
            "es" -> mapOf(
                "PRODUCTS" to "Compras", "HOME" to "Hogar", "PHARMACY" to "Farmacia",
                "AUTO" to "Auto", "PETS" to "Mascotas", "KIDS" to "Niños",
                "CLOTHING" to "Ropa", "ELECTRONICS" to "Electrónica", "OTHER" to "Otro",
            )
            "uk" -> mapOf(
                "PRODUCTS" to "Продукти", "HOME" to "Дім", "PHARMACY" to "Аптека",
                "AUTO" to "Авто", "PETS" to "Тварини", "KIDS" to "Діти",
                "CLOTHING" to "Одяг", "ELECTRONICS" to "Електроніка", "OTHER" to "Інше",
            )
            else -> mapOf(
                "PRODUCTS" to "Products", "HOME" to "Home", "PHARMACY" to "Pharmacy",
                "AUTO" to "Auto", "PETS" to "Pets", "KIDS" to "Kids",
                "CLOTHING" to "Clothing", "ELECTRONICS" to "Electronics", "OTHER" to "Other",
            )
        }
        return map[key] ?: map.getValue("OTHER")
    }

    fun shoppingStatus(key: String): String = when (lang()) {
        "ru" -> when (key) {
            "ACTIVE" -> "Активно"; "PURCHASED" -> "Куплено"; "ARCHIVED" -> "Архив"; else -> key
        }
        "sr" -> when (key) {
            "ACTIVE" -> "Aktivno"; "PURCHASED" -> "Kupljeno"; "ARCHIVED" -> "Arhiva"; else -> key
        }
        "de" -> when (key) {
            "ACTIVE" -> "Aktiv"; "PURCHASED" -> "Gekauft"; "ARCHIVED" -> "Archiv"; else -> key
        }
        "fr" -> when (key) {
            "ACTIVE" -> "Actif"; "PURCHASED" -> "Acheté"; "ARCHIVED" -> "Archives"; else -> key
        }
        "es" -> when (key) {
            "ACTIVE" -> "Activo"; "PURCHASED" -> "Comprado"; "ARCHIVED" -> "Archivo"; else -> key
        }
        "uk" -> when (key) {
            "ACTIVE" -> "Активно"; "PURCHASED" -> "Куплено"; "ARCHIVED" -> "Архів"; else -> key
        }
        else -> when (key) {
            "ACTIVE" -> "Active"; "PURCHASED" -> "Purchased"; "ARCHIVED" -> "Archived"; else -> key
        }
    }

    fun shoppingSort(key: String): String = when (lang()) {
        "ru" -> when (key) {
            "NAME_ASC" -> "Имя А–Я"; "NAME_DESC" -> "Имя Я–А"; "NEWEST" -> "Сначала новые"
            "OLDEST" -> "Сначала старые"; "PRICE_ASC" -> "Цена ↑"; "PRICE_DESC" -> "Цена ↓"
            "CATEGORY" -> "Категория"; else -> key
        }
        "sr" -> when (key) {
            "NAME_ASC" -> "Ime A–Š"; "NAME_DESC" -> "Ime Š–A"; "NEWEST" -> "Najnovije"
            "OLDEST" -> "Najstarije"; "PRICE_ASC" -> "Cena ↑"; "PRICE_DESC" -> "Cena ↓"
            "CATEGORY" -> "Kategorija"; else -> key
        }
        "de" -> when (key) {
            "NAME_ASC" -> "Name A–Z"; "NAME_DESC" -> "Name Z–A"; "NEWEST" -> "Neueste"
            "OLDEST" -> "Älteste"; "PRICE_ASC" -> "Preis ↑"; "PRICE_DESC" -> "Preis ↓"
            "CATEGORY" -> "Kategorie"; else -> key
        }
        "fr" -> when (key) {
            "NAME_ASC" -> "Nom A–Z"; "NAME_DESC" -> "Nom Z–A"; "NEWEST" -> "Plus récent"
            "OLDEST" -> "Plus ancien"; "PRICE_ASC" -> "Prix ↑"; "PRICE_DESC" -> "Prix ↓"
            "CATEGORY" -> "Catégorie"; else -> key
        }
        "es" -> when (key) {
            "NAME_ASC" -> "Nombre A–Z"; "NAME_DESC" -> "Nombre Z–A"; "NEWEST" -> "Más nuevo"
            "OLDEST" -> "Más antiguo"; "PRICE_ASC" -> "Precio ↑"; "PRICE_DESC" -> "Precio ↓"
            "CATEGORY" -> "Categoría"; else -> key
        }
        "uk" -> when (key) {
            "NAME_ASC" -> "Ім'я А–Я"; "NAME_DESC" -> "Ім'я Я–А"; "NEWEST" -> "Спочатку нові"
            "OLDEST" -> "Спочатку старі"; "PRICE_ASC" -> "Ціна ↑"; "PRICE_DESC" -> "Ціна ↓"
            "CATEGORY" -> "Категорія"; else -> key
        }
        else -> when (key) {
            "NAME_ASC" -> "Name A–Z"; "NAME_DESC" -> "Name Z–A"; "NEWEST" -> "Newest"
            "OLDEST" -> "Oldest"; "PRICE_ASC" -> "Price ↑"; "PRICE_DESC" -> "Price ↓"
            "CATEGORY" -> "Category"; else -> key
        }
    }

    fun taskStatus(key: String): String = when (lang()) {
        "ru" -> when (key) {
            "ALL" -> "Все"; "NEW" -> "Новая"; "IN_PROGRESS" -> "В работе"; "WAITING" -> "Ожидает"
            "DONE" -> "Выполнена"; "CANCELLED" -> "Отменена"; "OVERDUE" -> "Просрочена"; else -> key
        }
        "sr" -> when (key) {
            "ALL" -> "Sve"; "NEW" -> "Nova"; "IN_PROGRESS" -> "U toku"; "WAITING" -> "Na čekanju"
            "DONE" -> "Završena"; "CANCELLED" -> "Otkazana"; "OVERDUE" -> "Kasni"; else -> key
        }
        "de" -> when (key) {
            "ALL" -> "Alle"; "NEW" -> "Neu"; "IN_PROGRESS" -> "In Arbeit"; "WAITING" -> "Wartend"
            "DONE" -> "Erledigt"; "CANCELLED" -> "Abgebrochen"; "OVERDUE" -> "Überfällig"; else -> key
        }
        "fr" -> when (key) {
            "ALL" -> "Tous"; "NEW" -> "Nouveau"; "IN_PROGRESS" -> "En cours"; "WAITING" -> "En attente"
            "DONE" -> "Terminé"; "CANCELLED" -> "Annulé"; "OVERDUE" -> "En retard"; else -> key
        }
        "es" -> when (key) {
            "ALL" -> "Todos"; "NEW" -> "Nueva"; "IN_PROGRESS" -> "En progreso"; "WAITING" -> "Esperando"
            "DONE" -> "Hecha"; "CANCELLED" -> "Cancelada"; "OVERDUE" -> "Vencida"; else -> key
        }
        "uk" -> when (key) {
            "ALL" -> "Усі"; "NEW" -> "Нова"; "IN_PROGRESS" -> "В роботі"; "WAITING" -> "Очікує"
            "DONE" -> "Виконана"; "CANCELLED" -> "Скасована"; "OVERDUE" -> "Прострочена"; else -> key
        }
        else -> when (key) {
            "ALL" -> "All"; "NEW" -> "New"; "IN_PROGRESS" -> "In progress"; "WAITING" -> "Waiting"
            "DONE" -> "Done"; "CANCELLED" -> "Cancelled"; "OVERDUE" -> "Overdue"; else -> key
        }
    }

    fun taskPriority(key: String): String = when (lang()) {
        "ru" -> when (key) {
            "LOW" -> "Низкий"; "MEDIUM" -> "Средний"; "HIGH" -> "Высокий"; "URGENT" -> "Срочный"; else -> key
        }
        "sr" -> when (key) {
            "LOW" -> "Nizak"; "MEDIUM" -> "Srednji"; "HIGH" -> "Visok"; "URGENT" -> "Hitno"; else -> key
        }
        "de" -> when (key) {
            "LOW" -> "Niedrig"; "MEDIUM" -> "Mittel"; "HIGH" -> "Hoch"; "URGENT" -> "Dringend"; else -> key
        }
        "fr" -> when (key) {
            "LOW" -> "Bas"; "MEDIUM" -> "Moyen"; "HIGH" -> "Élevé"; "URGENT" -> "Urgent"; else -> key
        }
        "es" -> when (key) {
            "LOW" -> "Baja"; "MEDIUM" -> "Media"; "HIGH" -> "Alta"; "URGENT" -> "Urgente"; else -> key
        }
        "uk" -> when (key) {
            "LOW" -> "Низький"; "MEDIUM" -> "Середній"; "HIGH" -> "Високий"; "URGENT" -> "Терміновий"; else -> key
        }
        else -> when (key) {
            "LOW" -> "Low"; "MEDIUM" -> "Medium"; "HIGH" -> "High"; "URGENT" -> "Urgent"; else -> key
        }
    }

    fun recurrence(key: String): String = when (lang()) {
        "ru" -> when (key) {
            "DAILY" -> "Каждый день"; "WEEKLY" -> "Каждую неделю"
            "MONTHLY" -> "Каждый месяц"; "YEARLY" -> "Каждый год"; else -> key
        }
        "sr" -> when (key) {
            "DAILY" -> "Svaki dan"; "WEEKLY" -> "Svake nedelje"
            "MONTHLY" -> "Svaki mesec"; "YEARLY" -> "Svake godine"; else -> key
        }
        "de" -> when (key) {
            "DAILY" -> "Täglich"; "WEEKLY" -> "Wöchentlich"
            "MONTHLY" -> "Monatlich"; "YEARLY" -> "Jährlich"; else -> key
        }
        "fr" -> when (key) {
            "DAILY" -> "Quotidien"; "WEEKLY" -> "Hebdomadaire"
            "MONTHLY" -> "Mensuel"; "YEARLY" -> "Annuel"; else -> key
        }
        "es" -> when (key) {
            "DAILY" -> "Diario"; "WEEKLY" -> "Semanal"
            "MONTHLY" -> "Mensual"; "YEARLY" -> "Anual"; else -> key
        }
        "uk" -> when (key) {
            "DAILY" -> "Щодня"; "WEEKLY" -> "Щотижня"
            "MONTHLY" -> "Щомісяця"; "YEARLY" -> "Щороку"; else -> key
        }
        else -> when (key) {
            "DAILY" -> "Daily"; "WEEKLY" -> "Weekly"
            "MONTHLY" -> "Monthly"; "YEARLY" -> "Yearly"; else -> key
        }
    }

    fun eventType(key: String): String {
        val map = when (lang()) {
            "ru" -> mapOf(
                "BIRTHDAY" to "День рождения", "HOLIDAY" to "Праздник", "MEETING" to "Встреча",
                "TRIP" to "Поездка", "SCHOOL" to "Школа", "VET" to "Ветеринар",
                "DOCTOR" to "Врач", "BILL_PAYMENT" to "Оплата счетов", "OTHER" to "Другое",
            )
            "sr" -> mapOf(
                "BIRTHDAY" to "Rođendan", "HOLIDAY" to "Praznik", "MEETING" to "Sastanak",
                "TRIP" to "Putovanje", "SCHOOL" to "Škola", "VET" to "Veterinar",
                "DOCTOR" to "Doktor", "BILL_PAYMENT" to "Plaćanje računa", "OTHER" to "Ostalo",
            )
            "de" -> mapOf(
                "BIRTHDAY" to "Geburtstag", "HOLIDAY" to "Feiertag", "MEETING" to "Treffen",
                "TRIP" to "Reise", "SCHOOL" to "Schule", "VET" to "Tierarzt",
                "DOCTOR" to "Arzt", "BILL_PAYMENT" to "Rechnung", "OTHER" to "Sonstiges",
            )
            "fr" -> mapOf(
                "BIRTHDAY" to "Anniversaire", "HOLIDAY" to "Fête", "MEETING" to "Réunion",
                "TRIP" to "Voyage", "SCHOOL" to "École", "VET" to "Vétérinaire",
                "DOCTOR" to "Médecin", "BILL_PAYMENT" to "Facture", "OTHER" to "Autre",
            )
            "es" -> mapOf(
                "BIRTHDAY" to "Cumpleaños", "HOLIDAY" to "Festivo", "MEETING" to "Reunión",
                "TRIP" to "Viaje", "SCHOOL" to "Escuela", "VET" to "Veterinario",
                "DOCTOR" to "Médico", "BILL_PAYMENT" to "Pago", "OTHER" to "Otro",
            )
            "uk" -> mapOf(
                "BIRTHDAY" to "День народження", "HOLIDAY" to "Свято", "MEETING" to "Зустріч",
                "TRIP" to "Поїздка", "SCHOOL" to "Школа", "VET" to "Ветеринар",
                "DOCTOR" to "Лікар", "BILL_PAYMENT" to "Оплата рахунків", "OTHER" to "Інше",
            )
            else -> mapOf(
                "BIRTHDAY" to "Birthday", "HOLIDAY" to "Holiday", "MEETING" to "Meeting",
                "TRIP" to "Trip", "SCHOOL" to "School", "VET" to "Vet",
                "DOCTOR" to "Doctor", "BILL_PAYMENT" to "Bill payment", "OTHER" to "Other",
            )
        }
        return map[key] ?: map.getValue("OTHER")
    }

    fun budgetCategory(key: String): String {
        val map = when (lang()) {
            "ru" -> mapOf(
                "FOOD" to "Еда", "UTILITIES" to "Коммуналка", "CAR" to "Автомобиль",
                "EDUCATION" to "Образование", "HEALTH" to "Здоровье",
                "ENTERTAINMENT" to "Развлечения", "TRAVEL" to "Путешествия", "OTHER" to "Другое",
            )
            "sr" -> mapOf(
                "FOOD" to "Hrana", "UTILITIES" to "Komunalije", "CAR" to "Auto",
                "EDUCATION" to "Obrazovanje", "HEALTH" to "Zdravlje",
                "ENTERTAINMENT" to "Zabava", "TRAVEL" to "Putovanja", "OTHER" to "Ostalo",
            )
            "de" -> mapOf(
                "FOOD" to "Essen", "UTILITIES" to "Nebenkosten", "CAR" to "Auto",
                "EDUCATION" to "Bildung", "HEALTH" to "Gesundheit",
                "ENTERTAINMENT" to "Freizeit", "TRAVEL" to "Reisen", "OTHER" to "Sonstiges",
            )
            "fr" -> mapOf(
                "FOOD" to "Nourriture", "UTILITIES" to "Charges", "CAR" to "Voiture",
                "EDUCATION" to "Éducation", "HEALTH" to "Santé",
                "ENTERTAINMENT" to "Loisirs", "TRAVEL" to "Voyages", "OTHER" to "Autre",
            )
            "es" -> mapOf(
                "FOOD" to "Comida", "UTILITIES" to "Servicios", "CAR" to "Coche",
                "EDUCATION" to "Educación", "HEALTH" to "Salud",
                "ENTERTAINMENT" to "Ocio", "TRAVEL" to "Viajes", "OTHER" to "Otro",
            )
            "uk" -> mapOf(
                "FOOD" to "Їжа", "UTILITIES" to "Комуналка", "CAR" to "Авто",
                "EDUCATION" to "Освіта", "HEALTH" to "Здоров'я",
                "ENTERTAINMENT" to "Розваги", "TRAVEL" to "Подорожі", "OTHER" to "Інше",
            )
            else -> mapOf(
                "FOOD" to "Food", "UTILITIES" to "Utilities", "CAR" to "Car",
                "EDUCATION" to "Education", "HEALTH" to "Health",
                "ENTERTAINMENT" to "Entertainment", "TRAVEL" to "Travel", "OTHER" to "Other",
            )
        }
        return map[key] ?: map.getValue("OTHER")
    }

    fun calendarView(key: String): String = when (lang()) {
        "ru" -> when (key) {
            "MONTH" -> "Месяц"; "WEEK" -> "Неделя"; "DAY" -> "День"; "AGENDA" -> "Список"; else -> key
        }
        "sr" -> when (key) {
            "MONTH" -> "Mesec"; "WEEK" -> "Nedelja"; "DAY" -> "Dan"; "AGENDA" -> "Lista"; else -> key
        }
        "de" -> when (key) {
            "MONTH" -> "Monat"; "WEEK" -> "Woche"; "DAY" -> "Tag"; "AGENDA" -> "Agenda"; else -> key
        }
        "fr" -> when (key) {
            "MONTH" -> "Mois"; "WEEK" -> "Semaine"; "DAY" -> "Jour"; "AGENDA" -> "Agenda"; else -> key
        }
        "es" -> when (key) {
            "MONTH" -> "Mes"; "WEEK" -> "Semana"; "DAY" -> "Día"; "AGENDA" -> "Agenda"; else -> key
        }
        "uk" -> when (key) {
            "MONTH" -> "Місяць"; "WEEK" -> "Тиждень"; "DAY" -> "День"; "AGENDA" -> "Список"; else -> key
        }
        else -> when (key) {
            "MONTH" -> "Month"; "WEEK" -> "Week"; "DAY" -> "Day"; "AGENDA" -> "Agenda"; else -> key
        }
    }
}
