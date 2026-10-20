# Стеки вкладок сохраняются с именем класса ключа (Class.getName) и восстанавливаются через
# Class.forName. Стабильное имя ключа не даёт оптимизатору переименовать или объединить его класс;
# неиспользуемые ключи по-прежнему удаляются. Сериализатор ключа держат правила kotlinx-serialization.
-keepnames class * implements androidx.navigation3.runtime.NavKey
