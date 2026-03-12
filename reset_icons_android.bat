@echo off
echo =====================================
echo Limpando icones antigos do Android...
echo =====================================

cd app\src\main\res

echo Removendo pastas mipmap antigas...

rmdir /s /q mipmap-mdpi
rmdir /s /q mipmap-hdpi
rmdir /s /q mipmap-xhdpi
rmdir /s /q mipmap-xxhdpi
rmdir /s /q mipmap-xxxhdpi

echo Criando pastas novas...

mkdir mipmap-mdpi
mkdir mipmap-hdpi
mkdir mipmap-xhdpi
mkdir mipmap-xxhdpi
mkdir mipmap-xxxhdpi
mkdir mipmap-anydpi-v26

echo =====================================
echo Criando icones adaptive
echo =====================================

echo ^<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"^> > mipmap-anydpi-v26\ic_launcher.xml
echo     ^<background android:drawable="@drawable/ic_launcher_background"/^> >> mipmap-anydpi-v26\ic_launcher.xml
echo     ^<foreground android:drawable="@drawable/ic_launcher_foreground"/^> >> mipmap-anydpi-v26\ic_launcher.xml
echo ^</adaptive-icon^> >> mipmap-anydpi-v26\ic_launcher.xml

echo ^<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"^> > mipmap-anydpi-v26\ic_launcher_round.xml
echo     ^<background android:drawable="@drawable/ic_launcher_background"/^> >> mipmap-anydpi-v26\ic_launcher_round.xml
echo     ^<foreground android:drawable="@drawable/ic_launcher_foreground"/^> >> mipmap-anydpi-v26\ic_launcher_round.xml
echo ^</adaptive-icon^> >> mipmap-anydpi-v26\ic_launcher_round.xml

echo =====================================
echo Criando background do icone
echo =====================================

cd ..\drawable

echo ^<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle"^> > ic_launcher_background.xml
echo     ^<solid android:color="#25D366"/^> >> ic_launcher_background.xml
echo ^</shape^> >> ic_launcher_background.xml

echo =====================================
echo Criando foreground
echo =====================================

echo ^<layer-list xmlns:android="http://schemas.android.com/apk/res/android"^> > ic_launcher_foreground.xml
echo     ^<item^> >> ic_launcher_foreground.xml
echo         ^<bitmap android:gravity="center" android:src="@drawable/logo"/^> >> ic_launcher_foreground.xml
echo     ^</item^> >> ic_launcher_foreground.xml
echo ^</layer-list^> >> ic_launcher_foreground.xml

echo =====================================
echo Limpeza concluida!
echo =====================================
echo Agora rode:
echo gradlew clean
pause