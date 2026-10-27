@echo off
rem bichal-build-logic's tool: bichal --help (needs Python 3.11+).
where py >nul 2>nul || goto python
py -3 "%~dp0tools\bichal.py" %*
exit /b
:python
where python >nul 2>nul || goto missing
python "%~dp0tools\bichal.py" %*
exit /b
:missing
echo bichal needs Python 3.11 or newer: https://www.python.org/downloads/ 1>&2
exit /b 1
