# Правила приложения. Keep-правила для рефлексии лежат у модулей-владельцев (consumer-rules.pro
# Android-модуля или META-INF/proguard/ JVM-модуля), правила библиотек приходят из их артефактов.

# Номера строк в стектрейсах release; имя исходного файла заменяется общим, расшифровка — по mapping.txt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
