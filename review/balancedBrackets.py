def balanced_brackets(string):
    opening_brackets = "([{"
    closing_brackets = ")]}"

    stack = []
    start = 0
    end = len(string) - 1

    while start <= end:
        if string[start] in opening_brackets:
            stack.append(string[start])
        elif string[start] in closing_brackets:
            if len(stack) == 0:
                return False
            closing_bracket_index = closing_brackets.index(string[start])
            opening_bracket_index = opening_brackets.index(stack.pop())
            if opening_bracket_index != closing_bracket_index:
                return False
        start += 1

    if len(stack) == 0:
        return True
    else:
        return False


if __name__ == '__main__':
    word = "{([])}"
    print(balanced_brackets(word))
