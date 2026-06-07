# ⚡ WhatsApp Contact Remover
### Плавающее окно для удаления контактов из WhatsApp Business

---

## 📁 Структура проекта

```
WhatsAppContactRemover/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/kz/aitu/contactremover/
│       │   ├── model/
│       │   │   └── ContactRepository.kt       ← 100+ предустановленных контактов
│       │   ├── service/
│       │   │   ├── FloatingWindowService.kt   ← Основной overlay-сервис
│       │   │   └── ContactAccessibilityService.kt
│       │   ├── ui/
│       │   │   └── MainActivity.kt            ← Экран настроек и разрешений
│       │   └── utils/
│       │       └── PermissionHelper.kt
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── layout_floating_button.xml
│           │   ├── layout_panel.xml
│           │   └── item_contact.xml
│           ├── drawable/ (иконки и фоны)
│           ├── values/ (colors, strings, themes)
│           └── xml/accessibility_service_config.xml
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## 🛠️ Сборка через Android Studio

### Требования
- **Android Studio** Hedgehog 2023.1.1 или новее
- **JDK 17** (идёт в комплекте с Android Studio)
- **Android SDK** API 34

### Шаги

1. **Открыть проект**
   ```
   File → Open → выбрать папку WhatsAppContactRemover
   ```

2. **Подождать синхронизацию Gradle** (первый раз ~2–5 мин)

3. **Собрать Debug APK**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```
   APK будет в: `app/build/outputs/apk/debug/app-debug.apk`

4. **Установить на устройство**
   ```
   Build → Select Device → Run
   ```
   или через ADB:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🖥️ Сборка через командную строку

### macOS / Linux

```bash
# 1. Перейти в папку проекта
cd WhatsAppContactRemover

# 2. Дать права на выполнение gradlew
chmod +x gradlew

# 3. Собрать APK
./gradlew assembleDebug

# 4. APK находится по пути:
# app/build/outputs/apk/debug/app-debug.apk
```

### Windows

```cmd
cd WhatsAppContactRemover
gradlew.bat assembleDebug
```

### Установка через ADB

```bash
# Найти устройство
adb devices

# Установить
adb install app/build/outputs/apk/debug/app-debug.apk

# Или через USB (включить отладку по USB в настройках разработчика)
```

---

## 📱 Настройка приложения на устройстве

### 1. Разрешение "Поверх других приложений" ⚠️ ОБЯЗАТЕЛЬНО

```
Настройки → Приложения → Contact Remover
→ Особые права → Отображение поверх других приложений → ВКЛ
```
Или нажать кнопку **"Выдать"** в приложении.

### 2. Разрешения на контакты ⚠️ ОБЯЗАТЕЛЬНО

При первом запуске появится системный диалог — разрешить.

### 3. Служба специальных возможностей (опционально)

```
Настройки → Специальные возможности → Загруженные приложения
→ Contact Remover → ВКЛ
```
Позволяет автоматизировать нажатия в WhatsApp.

---

## 🚀 Использование

1. Запустить **Contact Remover**
2. Выдать все разрешения
3. Нажать **"▶ Запустить плавающую кнопку"**
4. Открыть **WhatsApp Business**
5. Нажать на **зелёную кнопку ✕**, которая плавает поверх экрана
6. Откроется панель:
   - Введите номер вручную **ИЛИ**
   - Найдите контакт через поиск и выберите из списка
7. Нажать **"🗑 УДАЛИТЬ КОНТАКТ"**
8. Откроется системный диалог удаления → подтвердить

---

## ⚙️ Механизм удаления

Приложение использует **3 метода** (в порядке приоритета):

| Метод | Описание |
|-------|----------|
| `ACTION_DELETE` + ContactsContract | Системный диалог удаления контакта по lookup key |
| `PhoneLookup` fallback | Поиск контакта по номеру, затем удаление |
| Ручной поиск | Открывает приложение Контакты с инструкцией |

---

## 📋 Предустановленные контакты

Приложение содержит **120+ контактов**:

| Оператор | Диапазон | Количество |
|----------|----------|------------|
| АЛТЕЛ | +7700 000 XXXX | 100 |
| АЛТЕЛ GSM | +7700 001/002/... | 5 |
| Beeline | +7705 | 5 |
| Kcell | +7702 | 5 |
| Tele2 | +7707 | 5 |

Добавить свои контакты: редактировать `ContactRepository.kt`, блок `presetContacts`.

---

## 🔧 Часто задаваемые вопросы

**Q: Плавающая кнопка не появляется**
A: Проверьте разрешение "Поверх других приложений". Это критичное разрешение.

**Q: "Контакт не найден" при удалении**
A: Контакт не сохранён в телефонной книге. Сначала добавьте его в контакты.

**Q: Приложение падает при запуске**
A: Убедитесь, что targetSdk и compileSdk = 34, minSdk = 24.

**Q: Как добавить больше контактов?**
A: Редактировать `ContactRepository.kt` — список `presetContacts`.

**Q: Поддерживает ли обычный WhatsApp (не Business)?**
A: Да. `PermissionHelper.buildWhatsAppIntent()` имеет fallback на `com.whatsapp`.

---

## 📦 Зависимости

```gradle
implementation 'androidx.core:core-ktx:1.12.0'
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
```

---

## 🔐 Разрешения AndroidManifest

| Разрешение | Назначение |
|------------|-----------|
| `SYSTEM_ALERT_WINDOW` | Отображение поверх всех окон |
| `READ_CONTACTS` | Поиск контакта по номеру |
| `WRITE_CONTACTS` | Удаление контакта |
| `FOREGROUND_SERVICE` | Фоновый сервис с уведомлением |
| `BIND_ACCESSIBILITY_SERVICE` | Автоматизация нажатий |

---

## 📝 Лицензия

Разработано для учебных целей. AITU CS-2508.
