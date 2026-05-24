---
name: unit-converter
description: Convert values between different units of measurement (length, weight, temperature, volume). Use this when the user asks to convert feet to meters, Celsius to Fahrenheit, pounds to kilograms, liters to gallons, etc.
---

# Unit Converter

This skill converts a numerical value from one unit of measurement to another entirely on-device.

## Instructions

Call the `run_js` tool with the following parameters:

- script name: index.html
- data: A JSON string with the following fields:
  - value: Number - the numerical value to convert (e.g., 30, 98.6)
  - fromUnit: String - the unit to convert from (e.g., "feet", "C", "lbs", "gallons")
  - toUnit: String - the unit to convert to (e.g., "meters", "F", "kg", "liters")
