#!/usr/bin/env python3
"""Разбор вывода `am instrument -r`: сводка с именами упавших тестов, JUnit XML и код возврата.

`am instrument` завершается с 0 и при упавших тестах, и при падении процесса, поэтому успех определяется
по выводу: каждый тест завершён без ошибки, есть итог «OK (N tests)» с N > 0 и INSTRUMENTATION_CODE: -1.

Использование: instrumentation_report.py <вывод am instrument> <JUnit XML> [имя набора]
Код возврата: 0 — все тесты прошли, 1 — есть падения или прогон не завершился.
"""
import os
import re
import sys
from xml.sax.saxutils import escape, quoteattr

# Коды INSTRUMENTATION_STATUS_CODE (androidx.test InstrumentationResultPrinter).
START, OK, ERROR, FAILURE, IGNORED, ASSUMPTION_FAILURE = 1, 0, -1, -2, -3, -4


def parse(lines):
    """Возвращает (тесты, итоговый stream, INSTRUMENTATION_CODE, сообщение о сбое процесса).

    Тест, начатый и не завершённый (процесс упал посреди него), попадает в список с кодом ERROR.
    """
    tests, status, result = [], {}, {}
    running = None
    code = None
    key, target = None, None
    for line in lines:
        line = line.rstrip("\r\n")
        m = re.match(r"INSTRUMENTATION_STATUS: ([^=]+)=(.*)", line)
        if m:
            key, target = m.group(1), status
            status[key] = m.group(2)
            continue
        m = re.match(r"INSTRUMENTATION_STATUS_CODE: (-?\d+)", line)
        if m:
            value = int(m.group(1))
            if value == START:
                running = dict(status)
            elif "test" in status:
                tests.append({**status, "code": value})
                running = None
            status, key, target = {}, None, None
            continue
        m = re.match(r"INSTRUMENTATION_RESULT: ([^=]+)=(.*)", line)
        if m:
            key, target = m.group(1), result
            result[key] = m.group(2)
            continue
        m = re.match(r"INSTRUMENTATION_CODE: (-?\d+)", line)
        if m:
            code = int(m.group(1))
            key, target = None, None
            continue
        if line.startswith("INSTRUMENTATION_FAILED"):
            result.setdefault("shortMsg", line)
            key, target = None, None
            continue
        # Продолжение многострочного значения (stack, stream).
        if target is not None and key is not None:
            target[key] += "\n" + line
    crash = result.get("shortMsg") or result.get("longMsg")
    if running is not None and "test" in running:
        tests.append({**running, "code": ERROR, "stack": f"Тест не завершился: {crash or 'прогон оборвался'}"})
    return tests, result.get("stream", ""), code, crash


def write_junit(path, suite, tests, crash):
    failed = [t for t in tests if t["code"] in (FAILURE, ERROR)]
    skipped = [t for t in tests if t["code"] in (IGNORED, ASSUMPTION_FAILURE)]
    errors = 1 if crash else 0
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "w", encoding="utf-8") as out:
        out.write('<?xml version="1.0" encoding="UTF-8"?>\n')
        out.write(
            f"<testsuite name={quoteattr(suite)} tests=\"{len(tests)}\" failures=\"{len(failed)}\" "
            f"errors=\"{errors}\" skipped=\"{len(skipped)}\" time=\"0\">\n"
        )
        for t in tests:
            out.write(f"  <testcase classname={quoteattr(t.get('class', ''))} name={quoteattr(t['test'])} time=\"0\">")
            stack = t.get("stack", "")
            if t["code"] in (FAILURE, ERROR):
                message = stack.splitlines()[0] if stack else ""
                out.write(f"\n    <failure message={quoteattr(message)}>{escape(stack)}</failure>\n  ")
            elif t["code"] in (IGNORED, ASSUMPTION_FAILURE):
                out.write("<skipped/>")
            out.write("</testcase>\n")
        if crash:
            out.write(f"  <testcase classname=\"instrumentation\" name=\"run\" time=\"0\">\n"
                      f"    <error message={quoteattr(crash)}/>\n  </testcase>\n")
        out.write("</testsuite>\n")


def main(argv):
    if len(argv) < 3:
        print(__doc__, file=sys.stderr)
        return 2
    raw, junit = argv[1], argv[2]
    suite = argv[3] if len(argv) > 3 else "instrumentation"
    with open(raw, encoding="utf-8", errors="replace") as f:
        tests, stream, code, crash = parse(f)

    passed = [t for t in tests if t["code"] == OK]
    failed = [t for t in tests if t["code"] in (FAILURE, ERROR)]
    skipped = [t for t in tests if t["code"] in (IGNORED, ASSUMPTION_FAILURE)]
    ok_line = re.search(r"^OK \((\d+) tests?\)", stream, re.MULTILINE)
    completed = code == -1 and ok_line is not None and int(ok_line.group(1)) > 0
    success = completed and not failed and not crash
    write_junit(junit, suite, tests, crash)

    print(f"Тестов: {len(tests)}, прошло: {len(passed)}, упало: {len(failed)}, пропущено: {len(skipped)}")
    for t in failed:
        stack = t.get("stack", "").splitlines()
        print(f"  УПАЛ {t.get('class', '')}#{t['test']}: {stack[0] if stack else ''}")
    if crash:
        print(f"Прогон прерван: {crash}")
    elif not completed and not failed:
        print(f"Прогон не завершён: INSTRUMENTATION_CODE={code}, итога «OK (N tests)» нет")
    return 0 if success else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
