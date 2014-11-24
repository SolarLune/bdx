import os
import re
import fnmatch
import bpy

p = os.path

def plugin_root():
    return p.dirname(__file__)

def gen_root():
    return p.join(plugin_root(), "gen")

proot = None 
def project_root():
    global proot
    if not proot:
        proot = p.join(bpy.path.abspath('//'), p.pardir)
    return p.abspath(proot)

def project_name():
    with open(p.join(project_root(), "build.gradle")) as f:
        for line in f.readlines():
            if "appName" in line:
                _, name, *_ = line.split("'")
                return name


def set_file_line(file_path, line_num, text):
    with open(file_path, 'r') as f:
        lines = f.readlines()

    lines[line_num - 1] = text + '\n'

    with open(file_path, 'w') as f:
        f.writelines(lines)

def get_file_line(file_path, line_num):
    with open(file_path, 'r') as f:
        lines = f.readlines()

    return lines[line_num - 1]


def set_file_var(file_path, var_name, value):
    with open(file_path, 'r') as f:
        lines = f.readlines()

    for i, ln in enumerate(lines):
        if var_name+" =" in ln:
            r, _ = ln.split('=')
            lines[i] = '= '.join([r, value + ';\n'])

    with open(file_path, 'w') as f:
        f.writelines(lines)


def remove_lines_containing(file_path, pattern):
    with open(file_path, 'r') as f:
        lines = [l for l in f.readlines() if pattern not in l]

    with open(file_path, 'w') as f:
        f.writelines(lines)


def insert_lines_after(file_path, pattern, new_lines):
    with open(file_path, 'r') as f:
        lines = f.readlines()

    for i, line in enumerate(lines):
        if pattern in line:
            break

    i += 1

    if i == len(lines):
        return

    new_lines = [l + '\n' for l in new_lines]

    lines = lines[:i] + new_lines + lines[i:]

    with open(file_path, 'w') as f:
        f.writelines(lines)


def replace_line_containing(file_path, pattern, new_line):
    with open(file_path, 'r') as f:
        lines = f.readlines()

    for i, line in enumerate(lines):
        if pattern in line:
            break

    lines[i] = new_line + '\n';

    with open(file_path, 'w') as f:
        f.writelines(lines)


def in_bdx_project():
    return p.isdir(p.join(project_root(), "android", "assets", "bdx"))

def dict_delta(d, dp):
    return {k: dp[k] for k in set(dp) - set(d)}

def src_root(project="core", target_file="BdxApp.java"):
    for root, dirs, files in os.walk(p.join(project_root(), project, "src")):
        if target_file in files:
            return root

def package_name():
    with open(p.join(src_root(), "BdxApp.java"), 'r') as f:
        _, package = f.readline().split()
    return package[:-1]

def angel_code(path_to_fnt):
    """
    Returns dict with relevant angel code data,
    which is retreived from a .fnt file.

    """
    def line_to_items(line):
        words = re.findall(r'(?:[^\s,"]|"(?:\\.|[^"])*")+', line)
        items = [w.split('=') if '=' in w else (w, "0") 
                 for w in words]
        return [(k, eval(v)) for k, v in items]

    ac = {"char":{}}
    with open(path_to_fnt, "r") as f:
        for l in f:
            (key, _), *data = line_to_items(l)
            if key == "char":
                (_, char_id), *rest = data
                ac["char"][char_id] = dict(rest)
            else:
                ac[key] = dict(data)
    
    return ac

def listdir_fullpath(d, ends_with_filter=None):
    paths = [p.join(d, f) for f in os.listdir(d)]
    if ends_with_filter:
        flt = ends_with_filter
        paths = [f for f in paths if f[-len(flt):] == flt]
    return paths

def gradle_cache_root():
    return p.join(p.expanduser('~'),
                  ".gradle",
                  "caches",
                  "modules-2",
                  "files-2.1")

def find_file(pattern, path):
    for root, dirs, files in os.walk(path):
        for name in files:
            if fnmatch.fnmatch(name, pattern):
                return p.join(root, name)

def libgdx_version():
    fp = p.join(project_root(), "build.gradle")
    _, version, *_ = get_file_line(fp, 20).split("'")
    return version

