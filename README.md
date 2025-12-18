# System Ekspercki: Rekomendator Dzieł Szekspira

## Autorzy

* Sebastian Zgoda - 147919
* Marcin Olter - 160095

Projekt realizujący system doradczy oparty na regułach (Rule-Based Expert System), stworzony w języku Java z wykorzystaniem silnika wnioskowania Drools. Aplikacja prowadzi interaktywny dialog z użytkownikiem, aby na podstawie jego nastroju, preferencji fabularnych i opinii zarekomendować odpowiednią sztukę Williama Szekspira.

## Architektura i Zgodność z Wymaganiami

Projekt został zaprojektowany ze ścisłym przestrzeganiem zasad inżynierii systemów eksperckich, ze szczególnym naciskiem na modułowość i czystość reguł.

### 1. Całkowita separacja wiedzy od interfejsu
* Warstwa Logiki (Model/Controller): Całe drzewo decyzyjne i mechanizm wnioskowania znajdują się wyłącznie w pliku shakespeare.drl. Kod Java nie zawiera żadnej wiedzy przedmiotowej ani sztywnych ścieżek sterujących.
* Warstwa Prezentacji (View): Klasa ShakespeareApp.java działa jako generyczny renderer. Jej zadaniem jest wyświetlanie pytań oraz prezentacja finalnej rekomendacji wraz z dedykowaną grafiką ładowaną dynamicznie.
* Zasoby Zewnętrzne i Internacjonalizacja: Teksty pytań, odpowiedzi oraz ścieżki do grafik są całkowicie odseparowane w plikach JSON. Pozwala to na obsługę dwóch wersji językowych (PL i EN) oraz łatwą wymianę grafik bez ingerencji w kod źródłowy czy reguły.

### 2. Mechanizm Wnioskowania
System unika sztywnych flag sterujących wymuszających kolejność kroków. Wnioskowanie opiera się na naturalnym przyroście faktów w pamięci roboczej:
* Reguły są kontekstowe – uruchamiają się w zależności od historii odpowiedzi użytkownika zgromadzonej w postaci faktów UserAnswer.
* Decyzje zapadają dynamicznie w trakcie trwania sesji, a nie poprzez wybór ze statycznej bazy danych.

### 3. Czystość Reguł
Zgodnie z wymaganiami projektowymi systemów regułowych:
* Sekcja then: Nie zawiera instrukcji warunkowych (if, switch) ani pętli. Służy wyłącznie do wstawiania nowych faktów (insert) lub operacji na obiektach domenowych.
* Sekcja when: Cała logika sterująca odbywa się poprzez dopasowywanie wzorców faktów.

### 4. Funkcjonalność Odrzucenia Rekomendacji (Furtka)
Zaimplementowano mechanizm obsługi odrzucenia rekomendacji. Gdy użytkownik wybierze opcję negatywną:
1. Wstawiany jest fakt UserRejected.
2. Reguła czyszcząca usuwa nietrafioną rekomendację z pamięci roboczej.
3. Uruchamia się alternatywna ścieżka wnioskowania zadająca dodatkowe pytanie.

## Wykorzystane Technologie

* Język: Java JDK 11
* Silnik Reguł: Drools 7.46 (KIE API)
* GUI: Java Swing (Dynamiczne generowanie interfejsu, wyświetlanie obrazów)
* Dane: Google Gson (Parsowanie zasobów JSON)
* IDE: Projekt kompatybilny ze strukturą Eclipse IDE.

## Przykładowy przebieg wnioskowania

1. Start: Reguła INIT wstawia fakt pytania Q_START.
2. Interakcja: Użytkownik wybiera opcję "Chcę popłakać" (OPT_CRY).
3. Fakt: Do pamięci roboczej trafia UserAnswer(id="Q_START", selection="OPT_CRY").
4. Reguła: Silnik dopasowuje regułę "Path: Cry -> Ask Revenge" i wstawia pytanie o zemstę.
5. Rekomendacja: Po serii pytań reguła generuje wniosek końcowy (np. REC_HAMLET), który jest prezentowany w interfejsie.
