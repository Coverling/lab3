#!/bin/bash

##############################################################################
# Упрощенный скрипт нагрузочного тестирования для отладки
##############################################################################

SERVICE_URL="http://localhost:8080/api/client/notifications"

echo "=========================================="
echo "Тест 1: Одиночный запрос"
echo "=========================================="
echo "URL: ${SERVICE_URL}?userId=1&limit=10"
echo ""

if curl -s -N "${SERVICE_URL}?userId=1&limit=10" --max-time 10 > /tmp/test_output.txt 2>&1; then
    echo "✓ Запрос выполнен успешно"
    echo "Количество строк в ответе: $(wc -l < /tmp/test_output.txt)"
else
    echo "✗ Ошибка при выполнении запроса"
    cat /tmp/test_output.txt
fi

echo ""
echo "=========================================="
echo "Тест 2: 10 последовательных запросов"
echo "=========================================="

success=0
errors=0
start_time=$(date +%s)

for i in {1..100000}; do
    user_id=$((1 + RANDOM % 10))
    if curl -s -N "${SERVICE_URL}?userId=${user_id}&limit=20" --max-time 10 > /dev/null 2>&1; then
        ((success++))
        echo "[$i/10] ✓ userId=${user_id}"
    else
        ((errors++))
        echo "[$i/10] ✗ userId=${user_id}"
    fi
done

end_time=$(date +%s)
duration=$((end_time - start_time))

echo ""
echo "Результаты:"
echo "  Успешно: ${success}"
echo "  Ошибок: ${errors}"
echo "  Время: ${duration} сек"
if [ $duration -gt 0 ]; then
    echo "  RPS: $((success / duration))"
fi

echo ""
echo "=========================================="
echo "Тест 3: 5 параллельных воркеров x 10 запросов"
echo "=========================================="

# Функция воркера
worker_simple() {
    local worker_id=$1
    local count=0
    for i in {1..10}; do
        user_id=$((1 + RANDOM % 10))
        if curl -s -N "${SERVICE_URL}?userId=${user_id}&limit=20" --max-time 10 > /dev/null 2>&1; then
            ((count++))
        fi
    done
    echo "Worker-${worker_id}: ${count}"
}

start_time=$(date +%s)

# Запуск воркеров
tmp_dir=$(mktemp -d)
for w in {1..5}; do
    worker_simple $w > "${tmp_dir}/worker_${w}.log" 2>&1 &
done

# Ожидание
wait

end_time=$(date +%s)
duration=$((end_time - start_time))

# Подсчет результатов
total=0
for w in {1..5}; do
    if [ -f "${tmp_dir}/worker_${w}.log" ]; then
        count=$(cat "${tmp_dir}/worker_${w}.log" | grep -oP 'Worker-\d+: \K\d+' || echo "0")
        total=$((total + count))
        echo "Worker-${w}: ${count} успешных запросов"
    fi
done

rm -rf "$tmp_dir"

echo ""
echo "Результаты:"
echo "  Всего успешных: ${total}"
echo "  Время: ${duration} сек"
if [ $duration -gt 0 ]; then
    echo "  RPS: $((total / duration))"
fi

echo ""
echo "=========================================="
echo "Если все тесты прошли успешно, можете запускать:"
echo "  bash load_test.sh"
echo "=========================================="
