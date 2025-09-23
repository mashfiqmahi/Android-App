import os
from fpdf import FPDF

# Folder containing Kotlin files
folder = r"D:\\mahi\\Android-App\\app\src\\main\\java\\com\\example\\androidbloodbank\\ui\\flow"

# Initialize PDF
pdf = FPDF()
pdf.set_auto_page_break(auto=True, margin=10)

# Load a Unicode font (Download DejaVuSans.ttf and put in same folder as this script)
pdf.add_font("DejaVu", "", "DejaVuSans.ttf", uni=True)
pdf.set_font("DejaVu", size=8)

# Walk through only .kt files
for root, _, files in os.walk(folder):
    for file in files:
        if file.endswith(".kt"):   # Only Kotlin files
            path = os.path.join(root, file)
            try:
                with open(path, "r", encoding="utf-8", errors="ignore") as f:
                    code = f.read()
            except Exception:
                continue

            pdf.add_page()
            pdf.multi_cell(0, 5, f"// File: {os.path.relpath(path, folder)}\n\n{code}")

# Save output
output_path = os.path.join(folder, "kotlin_code.pdf")
pdf.output(output_path)
print(f"✅ All Kotlin code exported to: {output_path}")
