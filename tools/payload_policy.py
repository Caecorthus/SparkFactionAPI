#!/usr/bin/env python3
"""Locked-player C2S payload policy: one canonical table, generated per-repo Java sets.

The canonical table is ``config/payload-policy/actions.tsv`` next to this tool (SparkFactionAPI). It holds one row
per payload id with an explicit ``allow``/``deny`` cell for every profile. Each repo gets a generated
``GeneratedPayloadPolicy`` class with one immutable ``Set<Identifier>`` per profile it uses (the ``deny`` ids); the
hand-written rule classes alias those sets. Standard library only.

    generate  --repo <path> --mod <factionapi|witch|strength>   write that repo's generated Java
    check     --repo <path> --mod <factionapi|witch|strength>   regenerate in memory, fail on any byte difference
                                                                or on a registered C2S id without a table row
    check-all --factionapi <path> --witch <path> --strength <path>

Table rules: UTF-8, tab-separated, LF, sorted by payload_id, one complete row per id (every profile cell exactly
``allow`` or ``deny``), ``owner`` is the registering mod id, ``note`` is ``-`` or the reason for an intentional
exception (a pinning test/comment, or ``migration-parity`` when the reason is unknown). VENDETTA_TARGET is not
emitted: VendettaTargetingPacketGuard keeps its typed adapters and the column records which payloads it judges.

锁定玩家 C2S 数据包策略：一张规范表，按仓库生成 Java 集合。规范表为本工具旁的 ``config/payload-policy/actions.tsv``
（SparkFactionAPI），每个数据包 id 一行，每个配置一格显式的 ``allow``/``deny``。每个仓库得到一个生成的
``GeneratedPayloadPolicy`` 类，为其使用的每个配置提供一个不可变 ``Set<Identifier>``（即 ``deny`` 的 id）；手写规则类
只是这些集合的别名。仅使用标准库。``check`` 在内存中重新生成并逐字节比较，且仓库注册了表中没有的 C2S id、或注册
表达式无法解析为字面量 id 时都会失败。
"""

from __future__ import annotations

import argparse
import hashlib
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

TOOL_PATH = Path(__file__).resolve()
TABLE_PATH = TOOL_PATH.parent.parent / "config" / "payload-policy" / "actions.tsv"
TABLE_DISPLAY = "config/payload-policy/actions.tsv"

PROFILES = (
    "SAFE_TIME", "STUN", "SEEKER", "RIFT", "FEAR", "FEAR_SILENT", "DUEL_PRIMARY", "DUEL_WEAPON",
    "HYPNOSIS_EXTRA", "HYPNOSIS_SILENT", "CARRY_EXTRA", "VENDETTA_TARGET", "DAZE",
)
HEADER = ("payload_id", "owner") + PROFILES + ("note",)
DECISIONS = ("allow", "deny")
OWNERS = ("noellesroles", "sparkfactionapi", "sparkstrength", "sparkwitch", "wathe")
ID_PATTERN = re.compile(r"[a-z0-9_.-]+:[a-z0-9_./-]+")


@dataclass(frozen=True)
class ModTarget:
    """Where one repo's generated class lives and which profiles it carries."""
    mod_id: str
    package: str
    public: bool
    profiles: tuple

    @property
    def relative_path(self) -> str:
        return "src/main/java/" + self.package.replace(".", "/") + "/GeneratedPayloadPolicy.java"


# SparkFactionAPI's only consumer shares the package, so its class stays package-private; SparkWitch and SparkStrength
# alias the sets from several role packages, so theirs must be public (still no cross-mod reference).
# SparkFactionAPI 唯一的使用方与生成类同包，因此保持包私有；SparkWitch 与 SparkStrength 在多个职业包中引用，必须公开
# （仍不跨模组引用）。
MODS = {
    "factionapi": ModTarget("sparkfactionapi", "dev.caecorthus.sparkfactionapi.impl.antifastround", False,
                            ("SAFE_TIME",)),
    "witch": ModTarget("sparkwitch", "dev.caecorthus.sparkwitch.payloadpolicy", True,
                       ("STUN", "SEEKER", "RIFT", "FEAR", "FEAR_SILENT", "DUEL_PRIMARY", "DUEL_WEAPON",
                        "HYPNOSIS_EXTRA", "HYPNOSIS_SILENT")),
    "strength": ModTarget("sparkstrength", "annina.sparkstrength.payloadpolicy", True, ("DAZE", "CARRY_EXTRA")),
}


class PolicyError(Exception):
    pass


# ---------------------------------------------------------------------------------------------------------------
# Canonical table / 规范表
# ---------------------------------------------------------------------------------------------------------------

@dataclass(frozen=True)
class Row:
    line: int
    payload_id: str
    owner: str
    decisions: dict
    note: str


@dataclass(frozen=True)
class Table:
    raw: bytes
    rows: tuple

    @property
    def sha256(self) -> str:
        return hashlib.sha256(self.raw).hexdigest()

    def denied(self, profile: str) -> list:
        return [row.payload_id for row in self.rows if row.decisions[profile] == "deny"]


def load_table(path: Path = TABLE_PATH) -> Table:
    try:
        raw = path.read_bytes()
    except OSError as error:
        raise PolicyError(f"{path}: cannot read canonical table: {error}") from error
    errors = []

    def fail(line, payload_id, column, message):
        where = f"{TABLE_DISPLAY}:{line}"
        if payload_id:
            where += f" id={payload_id}"
        if column:
            where += f" column={column}"
        errors.append(f"{where}: {message}")

    try:
        text = raw.decode("utf-8")
    except UnicodeDecodeError as error:
        raise PolicyError(f"{TABLE_DISPLAY}: not UTF-8: {error}") from error
    if "\r" in text:
        raise PolicyError(f"{TABLE_DISPLAY}: CR characters found; use LF line endings")
    if not text.endswith("\n"):
        raise PolicyError(f"{TABLE_DISPLAY}: the last line must end with a newline")
    lines = text[:-1].split("\n")
    header = tuple(lines[0].split("\t"))
    if header != HEADER:
        raise PolicyError(f"{TABLE_DISPLAY}:1: header must be exactly {' '.join(HEADER)!r}, found {' '.join(header)!r}")

    rows = []
    previous = None
    for number, line in enumerate(lines[1:], start=2):
        cells = line.split("\t")
        if len(cells) != len(HEADER):
            fail(number, cells[0] if cells else "", None,
                 f"expected {len(HEADER)} tab-separated cells, found {len(cells)}")
            continue
        payload_id, owner = cells[0], cells[1]
        if not ID_PATTERN.fullmatch(payload_id):
            fail(number, payload_id, "payload_id", "not a lowercase namespace:path identifier")
        if previous is not None and payload_id <= previous:
            fail(number, payload_id, "payload_id",
                 "duplicate id" if payload_id == previous else f"rows must be sorted by id (after {previous})")
        previous = payload_id
        if owner not in OWNERS:
            fail(number, payload_id, "owner", f"unknown owner {owner!r}; expected one of {', '.join(OWNERS)}")
        decisions = {}
        for profile, cell in zip(PROFILES, cells[2:2 + len(PROFILES)]):
            if cell not in DECISIONS:
                fail(number, payload_id, profile, f"decision must be exactly allow or deny, found {cell!r}")
            decisions[profile] = cell
        note = cells[-1]
        if not note or note != note.strip():
            fail(number, payload_id, "note", "must be '-' or text without leading or trailing whitespace")
        rows.append(Row(number, payload_id, owner, decisions, note))

    # The silent sets only mute the notice of an id the same lock already drops (see their Java comments).
    # 静默集合只会让同一锁已丢弃的 id 不再提示（见其 Java 注释）。
    for row in rows:
        d = row.decisions
        if d.get("FEAR_SILENT") == "deny" and d.get("FEAR") != "deny":
            fail(row.line, row.payload_id, "FEAR_SILENT", "a silently dropped id must also be denied in FEAR")
        if d.get("HYPNOSIS_SILENT") == "deny" and "deny" not in (d.get("STUN"), d.get("HYPNOSIS_EXTRA")):
            fail(row.line, row.payload_id, "HYPNOSIS_SILENT",
                 "a silently dropped id must also be denied in STUN or HYPNOSIS_EXTRA")
    if errors:
        raise PolicyError("\n".join(errors))
    return Table(raw, tuple(rows))


# ---------------------------------------------------------------------------------------------------------------
# Generation / 生成
# ---------------------------------------------------------------------------------------------------------------

def generate_java(table: Table, target: ModTarget, mod_key: str) -> str:
    public = "public " if target.public else ""
    out = [
        f"package {target.package};",
        "",
        "import net.minecraft.util.Identifier;",
        "",
        "import java.util.Set;",
        "",
        "/**",
        " * GENERATED FILE, DO NOT EDIT BY HAND. Generated from SparkFactionAPI"
        " {@code config/payload-policy/actions.tsv}",
        f" * by {{@code tools/payload_policy.py generate --mod {mod_key}}};"
        " {@code tools/payload_policy.py check} fails on any",
        " * difference. Each set holds the ids whose column in that table reads {@code deny}:"
        " the C2S payloads the profile",
        " * drops. Reasons for individual decisions are in the table's {@code note} column.",
        " * 生成文件，请勿手动编辑。由 SparkFactionAPI 的 {@code tools/payload_policy.py} 根据",
        " * {@code config/payload-policy/actions.tsv} 生成；任何差异都会使 {@code tools/payload_policy.py check} 失败。",
        " * 每个集合保存该表中对应列为 {@code deny} 的 id，即该配置丢弃的 C2S 数据包。各项决定的理由见表中的 {@code note} 列。",
        " */",
        f"{public}final class GeneratedPayloadPolicy {{",
        "    /** SHA-256 of the canonical table this file was generated from. / 生成本文件所用规范表的 SHA-256。 */",
        f"    {public}static final String CONTRACT_SHA256 = \"{table.sha256}\";",
    ]
    for profile in target.profiles:
        ids = table.denied(profile)
        if not ids:
            raise PolicyError(f"{TABLE_DISPLAY}: column {profile} denies nothing; refusing to emit an empty set")
        out.append("")
        out.append(f"    /** Column {profile}: {len(ids)} ids. / {profile} 列：{len(ids)} 个 id。 */")
        out.append(f"    {public}static final Set<Identifier> {profile} = Set.of(")
        for index, payload_id in enumerate(ids):
            namespace, path = payload_id.split(":", 1)
            end = "));" if index == len(ids) - 1 else "),"
            out.append(f"            Identifier.of(\"{namespace}\", \"{path}\"{end}")
    out += ["", "    private GeneratedPayloadPolicy() {", "    }", "}", ""]
    return "\n".join(out)


# ---------------------------------------------------------------------------------------------------------------
# Registration inventory: a small Java token resolver / 注册清单：小型 Java 记号解析器
# ---------------------------------------------------------------------------------------------------------------

TOKEN = re.compile(r"""
    (?P<ws>\s+)
  | (?P<comment>//[^\n]*|/\*.*?\*/)
  | (?P<textblock>\"\"\".*?\"\"\")
  | (?P<string>"(?:\\.|[^"\\\n])*")
  | (?P<char>'(?:\\.|[^'\\\n])+')
  | (?P<ident>[A-Za-z_$][A-Za-z0-9_$]*)
  | (?P<number>\d[\w.]*)
  | (?P<punct>->|::|\.\.\.|[^\sA-Za-z0-9_$])
""", re.S | re.X)

STRING_ESCAPES = {"n": "\n", "t": "\t", "\\": "\\", "\"": "\"", "'": "'", "r": "\r", "b": "\b", "f": "\f", "0": "\0"}


@dataclass(frozen=True)
class Tok:
    kind: str
    text: str
    pos: int


def tokenize(text: str, path: Path) -> list:
    tokens = []
    index = 0
    while index < len(text):
        match = TOKEN.match(text, index)
        if match is None:
            raise PolicyError(f"{path}: cannot tokenize near offset {index}: {text[index:index + 30]!r}")
        kind = match.lastgroup
        if kind not in ("ws", "comment"):
            tokens.append(Tok(kind, match.group(), index))
        index = match.end()
    return tokens


def string_value(token: Tok) -> str:
    body = token.text[1:-1]
    return re.sub(r"\\(.)", lambda m: STRING_ESCAPES.get(m.group(1), "\\" + m.group(1)), body)


@dataclass
class TypeDecl:
    fqcn: str
    name: str
    source: "SourceFile"
    outer: "TypeDecl | None"
    body: tuple  # (start, end) token indexes of the body braces
    nested: dict = field(default_factory=dict)
    fields: dict = field(default_factory=dict)   # name -> (start, end) initializer token range
    methods: dict = field(default_factory=dict)  # name -> list of (start, end) declaration token ranges


@dataclass
class SourceFile:
    path: Path
    text: str
    tokens: list
    package: str = ""
    imports: dict = field(default_factory=dict)          # simple name -> fqcn
    star_imports: list = field(default_factory=list)     # package or type prefixes
    static_imports: dict = field(default_factory=dict)   # member name -> owner fqcn
    types: list = field(default_factory=list)

    def line(self, token_index: int) -> int:
        return self.text.count("\n", 0, self.tokens[token_index].pos) + 1

    def where(self, token_index: int) -> str:
        return f"{self.path}:{self.line(token_index)}"


OPEN = {"(": ")", "{": "}", "[": "]"}
TYPE_KEYWORDS = {"class", "interface", "enum", "record"}


def matching(tokens: list, start: int) -> int:
    """Index of the bracket closing tokens[start]. / 与 tokens[start] 配对的右括号下标。"""
    stack = []
    for index in range(start, len(tokens)):
        text = tokens[index].text
        if tokens[index].kind == "punct" and text in OPEN:
            stack.append(OPEN[text])
        elif tokens[index].kind == "punct" and text in (")", "}", "]"):
            if not stack or stack.pop() != text:
                raise PolicyError(f"unbalanced {text!r} at token {index}")
            if not stack:
                return index
    raise PolicyError(f"unclosed bracket at token {start}")


class Repo:
    def __init__(self, root: Path):
        self.root = root
        self.files = []
        self.types = {}  # fqcn -> TypeDecl
        for base in ("src/main/java", "src/client/java"):
            directory = root / base
            if not directory.is_dir():
                continue
            for path in sorted(directory.rglob("*.java")):
                self.files.append(self._parse(path))

    # -- indexing -------------------------------------------------------------------------------------------------

    def _parse(self, path: Path) -> SourceFile:
        text = path.read_text(encoding="utf-8")
        source = SourceFile(path, text, tokenize(text, path))
        tokens = source.tokens
        index = 0
        while index < len(tokens):
            text_ = tokens[index].text
            if text_ == "package" and index == 0 or (text_ == "package" and tokens[index - 1].text in (";", "}")):
                end = self._until(tokens, index, ";")
                source.package = "".join(t.text for t in tokens[index + 1:end])
                index = end + 1
                continue
            if text_ == "import" and (index == 0 or tokens[index - 1].text == ";"):
                end = self._until(tokens, index, ";")
                parts = [t.text for t in tokens[index + 1:end]]
                is_static = parts and parts[0] == "static"
                if is_static:
                    parts = parts[1:]
                name = "".join(parts)
                if name.endswith(".*"):
                    if not is_static:
                        source.star_imports.append(name[:-2])
                elif is_static:
                    owner, member = name.rsplit(".", 1)
                    source.static_imports[member] = owner
                else:
                    source.imports[name.rsplit(".", 1)[-1]] = name
                index = end + 1
                continue
            break
        self._collect_types(source, index, len(tokens), None)
        return source

    @staticmethod
    def _until(tokens, start, text):
        for index in range(start, len(tokens)):
            if tokens[index].text == text:
                return index
        raise PolicyError(f"missing {text!r}")

    def _collect_types(self, source, start, end, outer):
        tokens = source.tokens
        index = start
        while index < end:
            token = tokens[index]
            if (token.kind == "ident" and token.text in TYPE_KEYWORDS and index + 1 < end
                    and tokens[index + 1].kind == "ident"
                    and (index == 0 or tokens[index - 1].text not in (".", "::"))
                    and not (token.text == "record" and tokens[index + 1].text in ("(",))):
                name = tokens[index + 1].text
                brace = index + 2
                while brace < end and tokens[brace].text != "{":
                    if tokens[brace].text == "(":
                        brace = matching(tokens, brace)
                    elif tokens[brace].text == ";":
                        break
                    brace += 1
                if brace >= end or tokens[brace].text != "{":
                    index += 1
                    continue
                close = matching(tokens, brace)
                fqcn = (outer.fqcn + "." + name) if outer else (source.package + "." + name if source.package else name)
                decl = TypeDecl(fqcn, name, source, outer, (brace, close))
                if fqcn in self.types:
                    raise PolicyError(f"{source.where(index)}: duplicate type {fqcn}")
                self.types[fqcn] = decl
                if outer:
                    outer.nested[name] = decl
                else:
                    source.types.append(decl)
                self._collect_members(source, decl)
                index = close + 1
                continue
            if token.kind == "punct" and token.text in OPEN:
                index = matching(tokens, index) + 1
                continue
            index += 1

    def _collect_members(self, source, decl):
        tokens = source.tokens
        start, close = decl.body
        index = start + 1
        statement_start = index
        while index < close:
            token = tokens[index]
            if token.kind == "ident" and token.text in TYPE_KEYWORDS and index + 1 < close \
                    and tokens[index + 1].kind == "ident" and tokens[index - 1].text not in (".", "::"):
                # Nested type: index it, then skip its body. / 嵌套类型：建立索引后跳过其主体。
                brace = index + 2
                while tokens[brace].text != "{":
                    if tokens[brace].text == "(":
                        brace = matching(tokens, brace)
                    brace += 1
                self._collect_types(source, index, matching(tokens, brace) + 1, decl)
                index = matching(tokens, brace) + 1
                statement_start = index
                continue
            if token.text == "=" and token.kind == "punct":
                name_token = tokens[index - 1]
                if name_token.kind == "ident":
                    end = index + 1
                    while end < close and tokens[end].text not in (";",):
                        if tokens[end].text in OPEN:
                            end = matching(tokens, end)
                        end += 1
                    decl.fields.setdefault(name_token.text, (index + 1, end))
                    index = end + 1
                    statement_start = index
                    continue
            if token.text == "(" and token.kind == "punct":
                name_token = tokens[index - 1]
                after = matching(tokens, index) + 1
                while after < close and tokens[after].text not in ("{", ";"):
                    after += 1
                if after < close and tokens[after].text == "{" and name_token.kind == "ident":
                    body_end = matching(tokens, after)
                    decl.methods.setdefault(name_token.text, []).append((statement_start, body_end))
                    index = body_end + 1
                    statement_start = index
                    continue
                index = matching(tokens, index) + 1
                continue
            if token.kind == "punct" and token.text == "{":
                index = matching(tokens, index) + 1
                statement_start = index
                continue
            if token.text == ";":
                statement_start = index + 1
            index += 1

    # -- name resolution ------------------------------------------------------------------------------------------

    def resolve_type(self, names: list, source: SourceFile, context: "TypeDecl | None"):
        """Longest prefix of ``names`` naming a repo type: (TypeDecl, consumed) or (None, 0). / 解析类型前缀。"""
        head = names[0]
        found = None
        scope = context
        while scope is not None and found is None:
            if head in scope.nested:
                found = scope.nested[head]
            elif scope.name == head:
                found = scope
            scope = scope.outer
        if found is None and head in source.imports:
            found = self.types.get(source.imports[head])
            if found is None:
                return None, 0  # an imported type outside this repo / 本仓库之外的导入类型
        if found is None:
            found = self.types.get((source.package + "." if source.package else "") + head)
        if found is None:
            for prefix in source.star_imports:
                candidate = self.types.get(prefix + "." + head)
                if candidate is not None:
                    found = candidate
                    break
        consumed = 1
        if found is None:
            # Fully qualified reference: package segments, then a type. / 全限定引用：包段之后是类型。
            for length in range(2, len(names) + 1):
                candidate = self.types.get(".".join(names[:length]))
                if candidate is not None:
                    found, consumed = candidate, length
                    break
            if found is None:
                return None, 0
        while consumed < len(names) and names[consumed] in found.nested:
            found = found.nested[names[consumed]]
            consumed += 1
        return found, consumed

    def find_field(self, owner: TypeDecl, name: str):
        scope = owner
        while scope is not None:
            if name in scope.fields:
                return scope, scope.fields[name]
            scope = scope.outer
        return None, None

    # -- expression evaluation ------------------------------------------------------------------------------------

    def evaluate(self, source: SourceFile, context, start: int, end: int, depth: int = 0):
        """Evaluate tokens[start:end] to ('str'|'id'|'payload', value). / 把记号区间求值为字符串、id 或负载 id。"""
        if depth > 40:
            raise PolicyError(f"{source.where(start)}: constant chain too deep")
        tokens = source.tokens
        text = " ".join(t.text for t in tokens[start:end])

        def unresolved(reason):
            return PolicyError(f"{source.where(start)}: cannot resolve {text!r} to a literal id ({reason})")

        if start >= end:
            raise unresolved("empty expression")
        # Strip enclosing parentheses. / 去掉外层括号。
        if tokens[start].text == "(" and matching(tokens, start) == end - 1:
            return self.evaluate(source, context, start + 1, end - 1, depth + 1)
        # Top-level string concatenation. / 顶层字符串拼接。
        parts, level, last = [], 0, start
        for index in range(start, end):
            t = tokens[index].text
            if t in OPEN:
                level += 1
            elif t in (")", "}", "]"):
                level -= 1
            elif t == "+" and level == 0:
                parts.append((last, index))
                last = index + 1
        if parts:
            parts.append((last, end))
            values = [self.evaluate(source, context, a, b, depth + 1) for a, b in parts]
            if any(kind != "str" for kind, _ in values):
                raise unresolved("non-string operand in concatenation")
            return "str", "".join(value for _, value in values)
        if end - start == 1 and tokens[start].kind == "string":
            return "str", string_value(tokens[start])
        # new Id<>(E) / new CustomPayload.Id<>(E)
        if tokens[start].text == "new":
            index = start + 1
            names = []
            while index < end and tokens[index].kind == "ident":
                names.append(tokens[index].text)
                if index + 1 < end and tokens[index + 1].text == ".":
                    index += 2
                else:
                    index += 1
                    break
            if names in (["Id"], ["CustomPayload", "Id"]) and index < end and tokens[index].text == "<":
                while index < end and tokens[index].text != ">":
                    index += 1
                index += 1
                if index < end and tokens[index].text == "(" and matching(tokens, index) == end - 1:
                    kind, value = self.evaluate(source, context, index + 1, end - 1, depth + 1)
                    if kind != "id":
                        raise unresolved("Id<> constructor argument is not an Identifier")
                    return "payload", value
            raise unresolved("unsupported constructor")
        # Dotted chain with an optional trailing call. / 点号链，末尾可带一次调用。
        names = []
        index = start
        while index < end and tokens[index].kind == "ident":
            names.append(tokens[index].text)
            if index + 1 < end and tokens[index + 1].text == ".":
                index += 2
            else:
                index += 1
                break
        if not names:
            raise unresolved("not a name, literal, call or Id<> constructor")
        call_args = None
        if index < end:
            if tokens[index].text == "(" and matching(tokens, index) == end - 1:
                call_args = (index + 1, end - 1)
            else:
                raise unresolved("unsupported expression shape")
        if call_args is not None:
            method = names[-1]
            receiver = names[:-1]
            args = self._split_args(source, *call_args)
            if receiver == ["Identifier"] or receiver == ["net", "minecraft", "util", "Identifier"]:
                if receiver == ["Identifier"] and source.imports.get("Identifier") != "net.minecraft.util.Identifier":
                    raise unresolved("Identifier is not net.minecraft.util.Identifier in this file")
                if method == "of" and len(args) == 2:
                    a = self._string(source, context, args[0], depth)
                    b = self._string(source, context, args[1], depth)
                    return "id", a + ":" + b
                if method == "of" and len(args) == 1:
                    value = self._string(source, context, args[0], depth)
                    if ":" not in value:
                        raise unresolved("single-argument Identifier.of without a namespace")
                    return "id", value
                raise unresolved(f"unsupported Identifier.{method}")
            if method == "id" and not args and receiver:
                # CustomPayload.Id#id(), e.g. SomePayload.ID.id(). / CustomPayload.Id 的 id() 访问器。
                kind, value = self._chain(source, context, receiver, start, depth)
                if kind != "payload":
                    raise unresolved(".id() on something that is not a CustomPayload.Id")
                return "id", value
            # A repo helper such as SparkWitch.id(String): return Identifier.of(CONST, path). / 仓库自带的 id 辅助方法。
            owner = context
            if receiver:
                owner, consumed = self.resolve_type(receiver, source, context)
                if owner is None or consumed != len(receiver):
                    raise unresolved(f"unknown method owner {'.'.join(receiver)}")
            else:
                while owner is not None and method not in owner.methods:
                    owner = owner.outer
                if owner is None:
                    raise unresolved(f"unknown method {method}")
            namespace = self._id_helper_namespace(owner, method)
            if namespace is None or len(args) != 1:
                raise unresolved(f"{owner.fqcn}.{method} is not a recognised Identifier.of(CONSTANT, path) helper")
            return "id", namespace + ":" + self._string(source, context, args[0], depth)
        return self._chain(source, context, names, start, depth)

    def _chain(self, source, context, names, start, depth):
        """Resolve a dotted constant reference. / 解析点号常量引用。"""
        def unresolved(reason):
            return PolicyError(f"{source.where(start)}: cannot resolve {'.'.join(names)!r} to a literal id ({reason})")

        if len(names) == 1:
            owner, span = self.find_field(context, names[0]) if context is not None else (None, None)
            if owner is None and names[0] in source.static_imports:
                imported = self.types.get(source.static_imports[names[0]])
                if imported is not None and names[0] in imported.fields:
                    owner, span = imported, imported.fields[names[0]]
            if owner is None:
                raise unresolved("no such constant in scope")
            return self.evaluate(owner.source, owner, span[0], span[1], depth + 1)
        owner, consumed = self.resolve_type(names, source, context)
        if owner is None:
            raise unresolved(f"{names[0]} is not a type of this repository")
        rest = names[consumed:]
        if len(rest) != 1:
            raise unresolved("expected Type.CONSTANT")
        return self._field_value(owner, rest[0], start, source, names, depth)

    def _field_value(self, owner, name, start, source, names, depth):
        if name not in owner.fields:
            raise PolicyError(f"{source.where(start)}: cannot resolve {'.'.join(names)!r}: {owner.fqcn} has no "
                              f"initialised field {name}")
        field_start, field_end = owner.fields[name]
        return self.evaluate(owner.source, owner, field_start, field_end, depth + 1)

    def _string(self, source, context, span, depth):
        kind, value = self.evaluate(source, context, span[0], span[1], depth + 1)
        if kind != "str":
            raise PolicyError(f"{source.where(span[0])}: expected a string constant, found {kind}")
        return value

    @staticmethod
    def _split_args(source, start, end):
        tokens = source.tokens
        args, level, last = [], 0, start
        for index in range(start, end):
            t = tokens[index].text
            if t in OPEN:
                level += 1
            elif t in (")", "}", "]"):
                level -= 1
            elif t == "," and level == 0:
                args.append((last, index))
                last = index + 1
        if last < end:
            args.append((last, end))
        return args

    def _id_helper_namespace(self, owner, method):
        """Namespace of ``static Identifier <method>(String p) { return Identifier.of(CONST, p); }``, else None."""
        declarations = owner.methods.get(method, [])
        if len(declarations) != 1:
            return None
        start, end = declarations[0]
        tokens = owner.source.tokens
        words = [t.text for t in tokens[start:end + 1]]
        try:
            open_paren = words.index("(")
        except ValueError:
            return None
        if "static" not in words[:open_paren] or words[open_paren - 2:open_paren] != ["Identifier", method]:
            return None
        signature = words[open_paren:open_paren + 4]
        if len(signature) != 4 or signature[1] != "String" or signature[3] != ")":
            return None
        parameter = signature[2]
        body = words[open_paren + 4:]
        expected_tail = ["{", "return", "Identifier", ".", "of", "(", None, ",", parameter, ")", ";", "}"]
        if len(body) != len(expected_tail) or any(e is not None and e != w for e, w in zip(expected_tail, body)):
            return None
        constant = body[6]
        if constant not in owner.fields:
            return None
        field_start, field_end = owner.fields[constant]
        kind, value = self.evaluate(owner.source, owner, field_start, field_end)
        return value if kind == "str" else None

    # -- registrations --------------------------------------------------------------------------------------------

    def c2s_registrations(self) -> list:
        """Every ``PayloadTypeRegistry.playC2S().register(id, codec)`` as (id, where). Fails on any other use."""
        found = []
        for source in self.files:
            tokens = source.tokens
            for index, token in enumerate(tokens):
                if token.kind != "ident" or token.text != "playC2S":
                    continue
                shape = [t.text for t in tokens[index:index + 6]]
                if shape != ["playC2S", "(", ")", ".", "register", "("]:
                    raise PolicyError(f"{source.where(index)}: playC2S() is used in a shape the inventory scan cannot "
                                      f"follow ({' '.join(shape)}); register directly with "
                                      f"PayloadTypeRegistry.playC2S().register(Type.ID, Type.CODEC)")
                open_paren = index + 5
                args = self._split_args(source, open_paren + 1, matching(tokens, open_paren))
                if len(args) != 2:
                    raise PolicyError(f"{source.where(index)}: playC2S().register expects two arguments")
                context = self._enclosing_type(source, index)
                kind, value = self.evaluate(source, context, *args[0])
                if kind != "payload":
                    raise PolicyError(f"{source.where(index)}: first register argument is not a CustomPayload.Id")
                found.append((value, source.where(index)))
        return found

    @staticmethod
    def _enclosing_type(source, token_index):
        best = None

        def visit(decl):
            nonlocal best
            if decl.body[0] < token_index < decl.body[1]:
                best = decl
                for nested in decl.nested.values():
                    visit(nested)

        for decl in source.types:
            visit(decl)
        return best


# ---------------------------------------------------------------------------------------------------------------
# Commands / 命令
# ---------------------------------------------------------------------------------------------------------------

def generate(repo: Path, mod_key: str) -> Path:
    table = load_table()
    target = MODS[mod_key]
    output = repo / target.relative_path
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(generate_java(table, target, mod_key).encode("utf-8"))
    return output


def check(repo: Path, mod_key: str) -> list:
    """All problems for one repo; empty means the repo matches the canonical table. / 返回该仓库的全部问题。"""
    problems = []
    table = load_table()
    target = MODS[mod_key]
    expected = generate_java(table, target, mod_key).encode("utf-8")
    output = repo / target.relative_path
    if not output.is_file():
        problems.append(f"{output}: missing; run tools/payload_policy.py generate --repo {repo} --mod {mod_key}")
    elif output.read_bytes() != expected:
        problems.append(f"{output}: differs from what {TABLE_DISPLAY} generates (sha256 {table.sha256}); "
                        f"regenerate it, never edit it by hand")
    rows = {row.payload_id: row for row in table.rows}
    try:
        registrations = Repo(repo).c2s_registrations()
    except PolicyError as error:
        problems.append(f"{repo}: registration scan failed: {error}")
        registrations = []
    for payload_id, where in registrations:
        row = rows.get(payload_id)
        if row is None:
            problems.append(f"{where}: registers C2S payload {payload_id}, which has no row in {TABLE_DISPLAY}; add a "
                            f"complete row with a deliberate allow/deny decision for every profile")
        elif row.owner != target.mod_id:
            problems.append(f"{where}: registers C2S payload {payload_id}, but {TABLE_DISPLAY}:{row.line} names "
                            f"owner {row.owner}, not {target.mod_id}")
    return problems


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    commands = parser.add_subparsers(dest="command", required=True)
    for name in ("generate", "check"):
        command = commands.add_parser(name)
        command.add_argument("--repo", required=True, type=Path)
        command.add_argument("--mod", required=True, choices=sorted(MODS))
    check_all = commands.add_parser("check-all")
    for mod_key in MODS:
        check_all.add_argument(f"--{mod_key}", required=True, type=Path)
    args = parser.parse_args(argv)
    try:
        if args.command == "generate":
            print(f"wrote {generate(args.repo, args.mod)}")
            return 0
        targets = [(args.mod, args.repo)] if args.command == "check" else [(key, getattr(args, key)) for key in MODS]
        failed = False
        for mod_key, repo in targets:
            problems = check(repo, mod_key)
            for problem in problems:
                print(f"[{mod_key}] {problem}", file=sys.stderr)
            print(f"[{mod_key}] {'FAILED' if problems else 'ok'}: {repo}")
            failed |= bool(problems)
        return 1 if failed else 0
    except PolicyError as error:
        print(error, file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
