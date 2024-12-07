# サンプルサーブレット

## 概要
SPIRAL PDF Generator を使ってPDFを生成するサンプルプログラムです。


## ビルド

1. サンプルサーブレットをビルドして WAR ファイルを作成します。

```
$ cd sample
$ ./gradlew build
```


2. ビルドに成功すれば、build/lib 以下に WAR ファイルが作成されます。

```
$ ls -l ./build/libs
spiral_pdf_sample.war
```


## インストール

1. WAR ファイルをTomcatのwebapp以下にコピーします。

```
sudo cp build/libs/spiral_pdf_sample.war /var/lib/tomcat9/webapps/
```


## 使い方

1. PDFテンプレートを用意する。差し替えキーワード email, name をサンプルのテキストデータで差し替えます。

2. ブラウザで http://hostname:8080/spiral_pdf_sample/Sample にアクセス

3. 用意したPDFテンプレートアップロード


以上
