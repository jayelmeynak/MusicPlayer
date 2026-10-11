#!/usr/bin/env python3
"""Разбор вывода `am instrument -r`: сводка с именами упавших тестов, JUnit XML и код возврата.

`am instrument` завершается с 0 и при упавших тестах, и при падении процесса, поэтому успех определяется
по выводу: ни один тест не упал, есть итог «OK (N tests)», где N > 0 и N = прошедшие + пропущенные,
и INSTRUMENTATION_CODE: -1.

Использование: instrumentation_report.py <вывод am instrument> <JUnit XML> [имя набора]
Код возврата: 0 — все тесты прошли, 1 — есть падения или прогон не завершился.
"""
import os
import re
import sys
from xml.sax.saxutils import escape, quoteattr

# Коды INSTRUMENTATION_STATUS_CODE (androidx.test InstrumentationResultPrinter).
START, IN_PROGRESS = 1, 2
OK, ERROR, FAILURE, IGNORED, ASSUMPTION_FAILURE = 0, -1, -2, -3, -4
# Только эти коды завершают тест; START, IN_PROGRESS и неизвестные — промежуточные статусы.
FINISHED = {OK, ERROR, FAILURE, IGNORED, ASSUMPTION_FAILURE}
FAILED = {ERROR, FAILURE}
SKIPPED = {IGNORED, ASSUMPTION_FAILURE}

# Управляющие символы, недопустимые в XML 1.0 (\t, \n, \r допустимы).
_CONTROL = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f]")


def clean(text):
    return _CONTROL.sub("", text)


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
            elif value in FINISHED and "test" in status:
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


def write_junit(path, suite, tests, problem):
    """problem — сбой прогона целиком (процесс упал, итога нет): отдельный синтетический testcase."""
    failures = sum(1 for t in tests if t["code"] == FAILURE)
    errors = sum(1 for t in tests if t["code"] == ERROR) + (1 if problem else 0)
    skipped = sum(1 for t in tests if t["code"] in SKIPPED)
    total = len(tests) + (1 if problem else 0)
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "w", encoding="utf-8") as out:
        out.write('<?xml version="1.0" encoding="UTF-8"?>\n')
        out.write(
            f"<testsuite name={quoteattr(clean(suite))} tests=\"{total}\" failures=\"{failures}\" "
            f"errors=\"{errors}\" skipped=\"{skipped}\" time=\"0\">\n"
        )
        for t in tests:
            name, cls = clean(t["test"]), clean(t.get("class", ""))
            out.write(f"  <testcase classname={quoteattr(cls)} name={quoteattr(name)} time=\"0\">")
            stack = clean(t.get("stack", ""))
            message = stack.splitlines()[0] if stack else ""
            if t["code"] in FAILED:
                tag = "failure" if t["code"] == FAILURE else "error"
                out.write(f"\n    <{tag} message={quoteattr(message)}>{escape(stack)}</{tag}>\n  ")
            elif t["code"] in SKIPPED:
                out.write("<skipped/>")
            out.write("</testcase>\n")
        if problem:
            out.write(f"  <testcase classname=\"instrumentation\" name=\"run\" time=\"0\">\n"
                      f"    <error message={quoteattr(clean(problem))}/>\n  </testcase>\n")
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
    failed = [t for t in tests if t["code"] in FAILED]
    skipped = [t for t in tests if t["code"] in SKIPPED]
    ok_line = re.search(r"^OK \((\d+) tests?\)", stream, re.MULTILINE)
    reported = int(ok_line.group(1)) if ok_line else None

    problem = crash
    if not problem and not failed:
        if code != -1 or reported is None:
            problem = f"Прогон не завершён: INSTRUMENTATION_CODE={code}, итога «OK (N tests)» нет"
        elif reported == 0:
            problem = "Ни одного теста: OK (0 tests)"
        elif reported != len(passed) + len(skipped):
            problem = (f"Итог OK ({reported} tests) не совпадает с разобранными статусами: "
                       f"прошло {len(passed)} + пропущено {len(skipped)}")
    success = not failed and not problem
    write_junit(junit, suite, tests, problem)

    print(f"Тестов: {len(tests)}, прошло: {len(passed)}, упало: {len(failed)}, пропущено: {len(skipped)}")
    for t in failed:
        stack = clean(t.get("stack", "")).splitlines()
        print(f"  УПАЛ {t.get('class', '')}#{t['test']}: {stack[0] if stack else ''}")
    if problem:
        print(f"Прогон прерван: {problem}" if crash else problem)
    return 0 if success else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
