# BZ Card Scan — User Guide

## Getting Started

When you first open the app, it will ask for **camera permission**. Tap "Grant Camera Permission" to proceed. The camera viewfinder will appear with a rectangular guide overlay in the center.

## Scanning a Business Card

1. Hold your phone over the business card
2. Position the card inside the **white rectangle guide** — only text inside this frame will be scanned
3. Make sure the card is:
   - **Well-lit** — avoid shadows and harsh glare
   - **Flat** — avoid curves or bends in the card
   - **Filling the frame** — get as close as possible while keeping the full card inside the rectangle
   - **Straight** — align the card edges with the rectangle
4. Tap the **camera button** at the bottom of the screen
5. The app will capture the image, crop it to the guide rectangle, and run text recognition

## Reviewing Results

After scanning, you'll see the **Scanned Card** screen with extracted fields:

- **Name** — Person's name
- **Job Title** — Their role or position
- **Company** — Organization name
- **Phone** — Phone number(s), comma-separated if multiple
- **Email** — Email address
- **Website** — Company or personal website
- **Address** — Street address

### Editing Fields

All fields are editable text boxes. Tap any field to correct the extracted text using your keyboard.

### Assigning Raw Text to Fields

Below the editable fields, you'll see the **raw scanned text** broken into individual lines. Each line has a small touch icon on the right.

To manually assign a line of text to a field:

1. **Tap the line** of raw text you want to use
2. A **dropdown menu** appears with all available fields (Name, Job Title, Company, Phone, Email, Website, Address)
3. **Select the field** you want to populate

Notes:
- For **Phone** and **Address**, tapping appends to the existing value (comma-separated) rather than replacing it, so you can build up multi-part values
- All other fields replace the current value when assigned

## Saving Cards

At the bottom of the results screen, you have three options:

- **Save & Add to Contacts** — Saves the card to the app's database AND exports it to your phone's contacts
- **Save Only** — Saves the card to the app's database without exporting to contacts
- **Discard** — Returns to the camera without saving anything

## Contacts Export & Account Selection

When exporting to contacts:

- **First time**: If you have multiple Google accounts, a picker dialog will appear asking which account to save to
- **Subsequent times**: The app remembers your last selection and uses it automatically
- **"Other..." button**: In the account picker, tap this to use the system's default contact chooser instead

## Viewing Saved Cards

Tap the **list icon** in the top-right corner of the camera screen to view all previously scanned cards.

The saved cards screen shows:
- Contact name, job title, company
- Phone number and email
- Date and time the card was scanned

### Actions on Saved Cards

Each card has two icon buttons:

- **Phone/contact icon** (blue) — Export this card to your phone's contacts
- **Trash icon** (red) — Delete the card from the app's database

## Tips for Better Scans

- **Lighting matters most** — Natural daylight or bright indoor lighting gives the best OCR results
- **Avoid reflective cards** — Glossy or metallic cards can cause glare; tilt slightly to reduce reflections
- **One card at a time** — Make sure only one card is visible inside the guide rectangle
- **Steady hands** — Hold the phone still when tapping the capture button
- **Retry if needed** — If the OCR misses text, tap Discard and try again with better positioning
- **Use tap-to-assign** — If the parser puts text in the wrong field, use the raw text lines to manually assign data to the correct fields
