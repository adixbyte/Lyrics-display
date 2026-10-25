
#include <LiquidCrystal.h>

LiquidCrystal lcd(12, 11, 5, 4, 3, 2);

char message[160];
int index = 0;

void setup() {
  Serial.begin(9600);
  lcd.begin(16, 2);
  lcd.clear();

  lcd.setCursor(0, 0);
  lcd.print("Lyrics Display");
  lcd.setCursor(0, 1);
  lcd.print("Waiting...");
}

void loop() {
  while (Serial.available() > 0) {
    char c = Serial.read();

    if (c == '\n') {
      message[index] = '\0';

      lcd.clear();
      lcd.setCursor(0, 0);

      // Display up to 32 characters over two rows.
      for (int i = 0; i < index && i < 32; i++) {
        if (i == 16) {
          lcd.setCursor(0, 1);
        }
        lcd.write(message[i]);
      }

      index = 0;
    } else if (c != '\r' && index < 159) {
      message[index++] = c;
    }
  }
}
