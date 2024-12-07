# SPIRAL PDF Generator Server

## 概要
キーワードを埋め込んだPDFテンプレートを用意しておき、指定したキーワードと値を動的に置換してPDFを生成するサーバーです。


## 動作環境
・Tomcat9

・Java8,11


## ビルド

1. SPIRAL PDF Generatorをビルドして WAR ファイルを作成します。

```
$ cd server
$ ./gradlew build
```


2. ビルドに成功すれば、build/libs 以下に WAR ファイルが作成されます。

```
$ ls -l ./build/libs
spiral_pdf_generator.war
```


## インストール

1. WAR ファイルをTomcatのwebapp以下にコピーします。

```
sudo cp build/libs/spiral_pdf_generator.war /var/lib/tomcat9/webapps/
```


## ライセンス
iTextライブラリを利用しているため、AGPLv3 のライブラリとします。

連絡先：SPIRAL株式会社


以上
