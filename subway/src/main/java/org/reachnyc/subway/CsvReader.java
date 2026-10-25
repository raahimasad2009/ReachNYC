package org.reachnyc.subway;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// reads one row at a time. can't just split(",") because some names have commas in quotes
public class CsvReader {

    private BufferedReader in;
    private Map<String, Integer> header = new HashMap<>();

    public CsvReader(Path file) throws IOException {
        in = Files.newBufferedReader(file, StandardCharsets.UTF_8);
        String first = in.readLine();
        if (first == null) return;
        if (first.startsWith("﻿")) first = first.substring(1);
        List<String> names = parseLine(first);
        for (int i = 0; i < names.size(); i++) {
            header.put(names.get(i).trim(), i);
        }
    }

    public Row next() throws IOException {
        String line = in.readLine();
        while (line != null && line.isEmpty()) {
            line = in.readLine();
        }
        if (line == null) {
            in.close();
            return null;
        }
        while (countQuotes(line) % 2 == 1) {
            String more = in.readLine();
            if (more == null) break;
            line = line + "\n" + more;
        }
        return new Row(header, parseLine(line));
    }

    public static class Row {
        private Map<String, Integer> header;
        private List<String> values;

        Row(Map<String, Integer> header, List<String> values) {
            this.header = header;
            this.values = values;
        }

        public String get(String column) {
            Integer i = header.get(column);
            if (i == null || i >= values.size()) return "";
            return values.get(i);
        }

        public boolean has(String column) {
            return header.containsKey(column);
        }
    }

    static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        String field = "";
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field += '"';
                    i++;
                } else if (c == '"') {
                    inQuotes = false;
                } else {
                    field += c;
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(field);
                field = "";
            } else if (c != '\r') {
                field += c;
            }
        }
        fields.add(field);
        return fields;
    }

    private static int countQuotes(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '"') count++;
        }
        return count;
    }
}
