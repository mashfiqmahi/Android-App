import os
from fpdf import FPDF

# Folder containing Kotlin files
folder = r"D:\\mahi\\Android-App\\app"

pdf = FPDF()
pdf.set_auto_page_break(auto=True, margin=10)

# Add Unicode-capable font (download DejaVuSans.ttf and place beside this script)
pdf.add_font("DejaVu", "", "DejaVuSans.ttf", uni=True)
pdf.set_font("DejaVu", size=8)

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
            # Print FULL file path above code
            pdf.multi_cell(0, 5, f"// File: {path}\n\n{code}")

# Save final PDF
output_path = os.path.join(folder, "kotlin_code.pdf")
pdf.output(output_path)
print(f"✅ All Kotlin code exported to: {output_path}")